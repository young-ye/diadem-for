package org.krasnow.cng.diadem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import org.krasnow.cng.data.ReadSWC;
import org.krasnow.cng.domain.BinaryTreeNode;
import org.krasnow.cng.domain.EuclideanPoint;
import org.krasnow.cng.domain.LinkedStack;
import org.krasnow.cng.domain.ParentedBinaryTreeNode;
import org.krasnow.cng.domain.SwcDataNode;
import org.krasnow.cng.domain.SwcSecondaryData;
import org.krasnow.cng.domain.SwcTreeNode;
import org.krasnow.cng.utils.BinaryTreeUtils;
import org.krasnow.cng.utils.SwcDataUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

/**
 * @author gillette
 * <p>
 * Calculates metric based on nodes and their connection to parent.
 * When parent is not found, searches up for match constrained by path length.
 * When node match is not found, searches up recursively for parent match,
 * then down for child match to determine continuation. Also constrained by
 * path length via error rate thresholds.
 */
public class DiademMetric {

    // Determines whether thresholds are micron or pixel based
    // Could be added as program parameter
    boolean microns = false;

    // Determines whether certain input parameters will be acceptable, and whether debugging is available
    private final static boolean testEnvironment = false;

    public final static String PARAM_TEST_DATA = "test-data";
    public final static String PARAM_GOLD_STANDARD_DATA = "gold-standard";
    public final static String PARAM_DATASET = "dataset";
    public final static String PARAM_WEIGHTED = "weighted";
    public final static String PARAM_XY_THRESHOLD = "xy-threshold";
    public final static String PARAM_Z_THRESHOLD = "z-threshold";
    public final static String PARAM_XY_PATH_THRESHOLD = "xyPathThresh";
    public final static String PARAM_Z_PATH_THRESHOLD = "zPathThresh";
    public final static String PARAM_Z_PATH = "z-path";
    public final static String PARAM_REMOVE_SPURS = "remove-spurs";

    public final static boolean DEFAULT_WEIGHTED = true;
    public final static double DEFAULT_XY_THRESHOLD = 1.2;
    public final static double DEFAULT_Z_THRESHOLD = 1;
    public final static double DEFAULT_PATH_LENGTH = 0.05;
    public final static boolean DEFAULT_Z_PATH = false;
    public final static double DEFAULT_REMOVE_SPURS = 1;

    public final static String PARAM_MISSES = "misses";
    public final static String PARAM_CONTINUATIONS = "continuations";
    public final static String PARAM_DISTANT_MATCHES = "distant-matches";

    public final static double DEFAULT_PATH_LENGTH_THRESHOLD = .05;

    public final static int DATASET_NONE = 0;
    public final static int DATASET_CCF = 1;
    public final static int DATASET_HC_CA3_INTERNEURON = 2;
    public final static int DATASET_NC_LAYER6_AXONS = 3;
    public final static int DATASET_NM_PROJECTION = 4;
    public final static int DATASET_OLFACTORY_PROJECTION = 5;

    private File testSwcFile;
    private File goldSwcFile;
    private int dataset = 0;
    private double XYThreshold = DEFAULT_XY_THRESHOLD;
    private double ZThreshold = DEFAULT_Z_THRESHOLD;
    private double XYPathErrorThreshold = DEFAULT_PATH_LENGTH_THRESHOLD;
    private double ZPathErrorThreshold = DEFAULT_PATH_LENGTH_THRESHOLD;
    private double LocalPathErrorThreshold = 0.4; // Approximate % error for hypotenuse to sides
    private boolean weighted = true;
    private boolean debug = false;
    private boolean listMisses = false;
    private boolean listContinuations = false;
    private boolean listDistantMatches = false;
    // Determines whether Z component is considered while calculating path length
    private boolean zPath = false;
    // Multiplier of Z component to keep XY and Z components in the same units
    private double scaleZ = 1;
    // Used to see debug details for node(s) at xy coordinates
    private double x;
    private double y;
    SwcDataNode xyCheck = new SwcDataNode();
    // Threshold under which terminal nodes are removed
    private double removeSpurs = 0;
    // Set based on whether current/target node is which XY threshold of x and y parameters
    boolean writeDetails;

    private boolean calculated;

    private List misses = new ArrayList();
    private List continuations = new ArrayList();
    private List distantMatches = new ArrayList();
    private List spurList = new ArrayList();
    private Map weightMap = new HashMap();
    private Map excessNodes = new LinkedHashMap();

    private double scoreSum, weightSum, quantityScoreSum;

    private double directMatchScore;
    private double qualityScore;
    private double finalScore;

    // Specifically for Neuromuscular Projection Fibers (dataset 4)
    private final static double ROSETTE_THRESHOLD = 10;
    private Map testTreePathNodes = null;

    // For ensuring nodes aren't used multiple times
    private Map matches = new HashMap();

    public DiademMetric(File testSwcFile, File goldSwcFile, int dataset) {
        this.testSwcFile = testSwcFile;
        this.goldSwcFile = goldSwcFile;
        setDataset(dataset);
        calculated = false;
    }

    public DiademMetric(File testSwcFile, File goldSwcFile) {
        this.testSwcFile = testSwcFile;
        this.goldSwcFile = goldSwcFile;
        calculated = false;
    }

    public void setDataset(int dataset) {
        this.dataset = dataset;
        double pixelPerMic = 1;

        switch (dataset) {
            case DATASET_CCF:
			/* 26.67 pixels/micron, 0.0375 microns/pixel
			   3 images/micron, 0.33 microns/image */
                pixelPerMic = 26.67;
                // Units in pixels and images
                XYThreshold = 37.33;
                scaleZ = 8.8; // pixels/image
                ZThreshold = 4 * scaleZ; // ZThresh in pixels

                XYPathErrorThreshold = 0.075;
                ZPathErrorThreshold = 0.18;
                zPath = true;
                // No spur removal
                removeSpurs = 0;
                break;
            case DATASET_HC_CA3_INTERNEURON:
			/* 4.6 pixels/micron, 0.217391 microns/pixel
			   3 images/micron, 0.33 microns/image */
                pixelPerMic = 4.6;

                XYThreshold = 11;
                scaleZ = 1.52; // pixels/image
                //ZThreshold = 3 * scaleZ; // ZThresh in pixels
                ZThreshold = 14 * scaleZ; // ZThresh in pixels
                XYPathErrorThreshold = 0.08;
                // Z path length error is not to be constrained
                ZPathErrorThreshold = 100;
                zPath = false;

                removeSpurs = 23;
                break;
            case DATASET_NC_LAYER6_AXONS:
			/* 3.4 pixels/micron, 0.2941 microns/pixel
			   1 images/micron, 1 microns/image */
                pixelPerMic = 3.4;

                XYThreshold = 4.76;
                scaleZ = 3.4; // pixels/image
                ZThreshold = 5 * scaleZ; // ZThresh in pixels
                XYPathErrorThreshold = 0.07;
                ZPathErrorThreshold = 0.18;

                removeSpurs = 17;
                break;
            case DATASET_NM_PROJECTION:
			/* 26.67 pixels/micron, 0.0375 microns/pixel
			   5 images/micron, 0.2 microns/image */
                pixelPerMic = 26.67;

                XYThreshold = 32;
                XYPathErrorThreshold = 0.04;
                // Z path error will be zero, threshold is irrelevant
                ZPathErrorThreshold = 100;
                scaleZ = 0;
                removeSpurs = 0;
                break;
            case DATASET_OLFACTORY_PROJECTION:
			/* 3.034 pixels/micron, 0.3296485 microns/pixel
			   1 images/micron, 1 microns/image */
                pixelPerMic = 3.034;

                XYThreshold = 3.94;
                scaleZ = 3.034; // pixels/image
                ZThreshold = 5 * scaleZ; // ZThresh in pixels

                XYPathErrorThreshold = 0.08;
                ZPathErrorThreshold = 0.20;

                zPath = true;
                removeSpurs = 6;
                break;
        }
        if (microns) {
            XYThreshold = XYThreshold / pixelPerMic;
            ZThreshold = ZThreshold / pixelPerMic;
            removeSpurs = removeSpurs / pixelPerMic;
        }
        if (testEnvironment) System.out.println("XYThreshold: " + XYThreshold + "; ZThreshold: " + ZThreshold);
    }

    public int getDataset() {
        return dataset;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isListContinuations() {
        return listContinuations;
    }

    public void setListContinuations(boolean listContinuations) {
        this.listContinuations = listContinuations;
    }

    public boolean isListDistantMatches() {
        return listDistantMatches;
    }

    public void setListDistantMatches(boolean listDistantMatches) {
        this.listDistantMatches = listDistantMatches;
    }

    public boolean isListMisses() {
        return listMisses;
    }

    public void setListMisses(boolean listMisses) {
        this.listMisses = listMisses;
    }

    public File getGoldSwcFile() {
        return goldSwcFile;
    }

    public void setGoldSwcFile(File goldSwcFile) {
        this.goldSwcFile = goldSwcFile;
        calculated = false;
    }

    public boolean isWeighted() {
        return weighted;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public File getTestSwcFile() {
        return testSwcFile;
    }

    public void setTestSwcFile(File testSwcFile) {
        this.testSwcFile = testSwcFile;
        calculated = false;
    }

    public double getXYThreshold() {
        return XYThreshold;
    }

    public void setXYThreshold(double threshold) {
        XYThreshold = threshold;
    }

    public double getZThreshold() {
        return ZThreshold;
    }

    public void setZThreshold(double threshold) {
        ZThreshold = threshold;
    }

    public double getXYPathErrorThreshold() {
        return XYPathErrorThreshold;
    }

    public void setXYPathErrorThreshold(double pathLengthThreshold) {
        XYPathErrorThreshold = pathLengthThreshold;
    }

    public double getZPathErrorThreshold() {
        return ZPathErrorThreshold;
    }

    public boolean isZPath() {
        return zPath;
    }

    public void setZPath(boolean path) {
        zPath = path;
    }

    public double getRemoveSpurs() {
        return removeSpurs;
    }

    public void setRemoveSpurs(double removeSpurs) {
        this.removeSpurs = removeSpurs;
    }

    public void setZPathErrorThreshold(double pathLengthThreshold) {
        ZPathErrorThreshold = pathLengthThreshold;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public double getScaleZ() {
        return scaleZ;
    }

    public void scoreReconstruction() throws Exception {

        SwcTreeNode testTreeRoot, goldTreeRoot;
        List goldFileList = new ArrayList(), testFileList = new ArrayList();

        // Gets all gold standard and test swc files in the directories
        if (goldSwcFile.isDirectory()) {
            File[] fileList = goldSwcFile.listFiles();
            File goldFile, testFile;
            String filename;
            for (int i = 0; i < fileList.length; i++) {
                goldFile = fileList[i];
                if (!goldFile.isDirectory()) {
                    filename = goldFile.getName();
                    if (filename.endsWith(".swc") || filename.endsWith(".SWC")) {
                        testFile = new File(testSwcFile.getAbsolutePath() + "/" + filename);
                        if (testFile.exists()) {
                            goldFileList.add(goldFile);
                            testFileList.add(testFile);
                        }
                    }
                }
            }
        } else {
            // For case in which parameter values are swc files rather than directories
            goldFileList.add(goldSwcFile);
            testFileList.add(testSwcFile);
        }

        scoreSum = quantityScoreSum = 0;

        // Loop through and read in all swc files
        for (int i = 0; i < goldFileList.size(); i++) {
            goldSwcFile = (File) goldFileList.get(i);
            testSwcFile = (File) testFileList.get(i);

            SwcDataNode thresholds = new SwcDataNode();
            thresholds.setX(XYThreshold);
            thresholds.setZ(ZThreshold);
            goldTreeRoot = ReadSWC.convertSwcToBinaryTreeList(goldSwcFile, zPath, scaleZ, thresholds);
            testTreeRoot = ReadSWC.convertSwcToBinaryTreeList(testSwcFile, zPath, scaleZ, thresholds);
            if (dataset == DATASET_NM_PROJECTION) {
                // Necessary for handling terminations within rossettes
                testTreePathNodes = ReadSWC.convertSwcToTreeNodeMap(testSwcFile, scaleZ);
            }
            // target nodes near xyCheck will display detailed information (only in test environment)
            xyCheck.setX(getX());
            xyCheck.setY(getY());

            if (removeSpurs > 0) {
                // Only remove spurs of gold standard. Remove test spurs prior to determining excess
                removeSpurs(goldTreeRoot, removeSpurs);
            }
            generateNodeWeights(goldTreeRoot);

            // This is where the real business is done
            scoreTrees(testTreeRoot, goldTreeRoot);
        }

        // Present output
        NumberFormat nf = NumberFormat.getInstance();
        System.out.println("Score: " + nf.format(finalScore));

        //存结果
        File result_file = null;
        FileWriter fw = null;
        result_file = new File("F:\\u\\result\\metric12\\diadem\\dataset\\dataset_5_result.txt");
        if (!result_file.exists()) {
            result_file.createNewFile();
        }
        BufferedWriter output = new BufferedWriter(new FileWriter(result_file, true));
        output.write("Score: " + nf.format(finalScore) + '\n');
        output.flush();
        output.close();

        // List misses
        ParentedBinaryTreeNode node;
        if (listMisses) {
            System.out.println();
            if (misses.size() > 0) {
                System.out.println("Nodes that were missed (position and weight):");
                for (int i = 0; i < misses.size(); i++) {
                    node = (ParentedBinaryTreeNode) misses.get(i);
                    System.out.println(getPositionString(node) + " " + weightMap.get(node));
                }
            } else {
                System.out.println("Nodes that were missed: none");
            }

            System.out.println();
            if (excessNodes.size() > 0) {
                System.out.println("Extra nodes in test reconstruction (position and weight):");
                for (Iterator it = excessNodes.keySet().iterator(); it.hasNext(); ) {
                    node = (ParentedBinaryTreeNode) it.next();
                    System.out.println(getPositionString(node) + " " + excessNodes.get(node));
                }
            } else {
                System.out.println("Extra nodes in test reconstruction: none");
            }
        }

        // Present output
        if (testEnvironment) {
            System.out.println("Direct hit score: " + nf.format(directMatchScore));
            System.out.println((weighted ? "Weighted " : "") + "Score including continuations: " + nf.format(qualityScore));
            System.out.println("Total adjusted for excess nodes: " + nf.format(finalScore));

            // List distant matches
            if (listDistantMatches) {
                System.out.println();
                if (distantMatches.size() > 0) {
                    System.out.println("Distant Matches:");
                    for (int i = 0; i < distantMatches.size(); i++) {
                        node = (ParentedBinaryTreeNode) distantMatches.get(i);
                        System.out.println(getPositionString(node) + " " + node.getDegree());
                    }
                } else {
                    System.out.println("Distant Matches: none");
                }
            }
            // List continuations
            if (listContinuations) {
                System.out.println();
                if (continuations.size() > 0) {
                    System.out.println("Continuation nodes (position and weight):");
                    for (int i = 0; i < continuations.size(); i++) {
                        node = (ParentedBinaryTreeNode) continuations.get(i);
                        System.out.println(getPositionString(node) + " " + weightMap.get(node));
                    }
                } else {
                    System.out.println("Continuation nodes: none");
                }
            }
        }
    }

    /**
     * @param testRoot
     * @param goldRoot
     * @return
     */
    public void scoreTrees(
            SwcTreeNode testRoot, SwcTreeNode goldRoot) throws Exception {
        ParentedBinaryTreeNode match;

        // Weights
        double weight;
        List testTrees = testRoot.getChildren();
        List goldTrees = goldRoot.getChildren();
        int numberOfNodes = 0;

        SwcDataNode goldData;

        // For roots, create nodes and set as match
        goldRoot.getSwcData().setSecondaryData(new SwcSecondaryData());
        // Root is coregistered and so will have no trajectory calculation (trajectory is self for simplicity)
        goldRoot.getSwcData().getSecondaryData().setLeftTrajectoryPoint(goldRoot.getSwcData());
        goldRoot.getSwcData().getSecondaryData().setRightTrajectoryPoint(goldRoot.getSwcData());
        goldRoot.getSwcData().getSecondaryData().setPathLength(0);
        ParentedBinaryTreeNode trueBaselineRoot = new ParentedBinaryTreeNode(goldRoot.getSwcData());
        testRoot.getSwcData().setSecondaryData(new SwcSecondaryData());
        testRoot.getSwcData().getSecondaryData().setLeftTrajectoryPoint(testRoot.getSwcData());
        testRoot.getSwcData().getSecondaryData().setRightTrajectoryPoint(testRoot.getSwcData());
        testRoot.getSwcData().getSecondaryData().setPathLength(0);
        ParentedBinaryTreeNode trueSampleRoot = new ParentedBinaryTreeNode(testRoot.getSwcData());
        matches.put(trueBaselineRoot, trueSampleRoot);

        // Traverses test tree and returns a list of nodes
        List testNodeList = new ArrayList();
        ParentedBinaryTreeNode rootNode;
        for (int i = 0; i < testTrees.size(); i++) {
            rootNode = (ParentedBinaryTreeNode) testTrees.get(i);
            testNodeList.addAll(
                    BinaryTreeUtils.createNodeList(rootNode));
            // Give first node a stable parent, the root of all trees should be a given
            rootNode.setParent(trueSampleRoot, false);
        }

        // Traverses gold tree and returns a list of nodes
        List goldNodeList = new ArrayList();
        for (int i = 0; i < goldTrees.size(); i++) {
            rootNode = (ParentedBinaryTreeNode) goldTrees.get(i);
            goldNodeList.addAll(
                    BinaryTreeUtils.createNodeList(rootNode));
            // Give first node a stable parent, the root of all trees should be a given
            rootNode.setParent(trueBaselineRoot, false);
        }

        // Stack used to keep track of nodes to be scored
        LinkedStack stack = new LinkedStack();
        ParentedBinaryTreeNode goldNode;

        numberOfNodes -= spurList.size();

        // Score nodes in each gold standard tree
        for (int i = 0; i < goldTrees.size(); i++) {
            goldNode = (ParentedBinaryTreeNode) goldTrees.get(i);

            numberOfNodes += goldNode.getTreeSize();

            // Score each node (including "root", which is the first bifurcation)
            stack.push(goldNode);

            while (!stack.isEmpty()) {
                goldNode = (ParentedBinaryTreeNode) stack.pop();
                goldData = (SwcDataNode) goldNode.getData();

                // Add this node's children to the stack
                if (goldNode.hasChildren()) {
                    stack.push(goldNode.getParentedLeft());
                    stack.push(goldNode.getParentedRight());
                }

                // Determine whether to write details
                writeDetails = false;
                if (testEnvironment && isWithinXYThreshold(xyCheck, goldData)) {
                    writeDetails = true;
                }

                // Only score node if it isn't a spur or single spur parent
                if (spurList.contains(goldNode)) {
                    if (writeDetails)
                        System.out.println("\nBaselineNode: " + getPositionString(goldNode) + " is a spur");
                } else {
                    // Set weight
                    weight = this.isWeighted() ? ((Integer) weightMap.get(goldNode)).intValue() : 1;
                    weightSum += weight;

                    if (writeDetails || debug) {
                        System.out.println("\nBaselineNode: " + getPositionString(goldNode));
                        System.out.println("Degree: " + goldNode.getDegree());
                    }

                    // Find match for target node (search for potentials, confirmation, and selection of best)
                    if (dataset == DATASET_NM_PROJECTION && goldNode.isLeaf()) {
                        match = getNMPTerminationMatch(goldNode, goldNodeList, testNodeList);
                    } else {
                        match = getClosestMatch(goldNode, goldNodeList, testNodeList);
                    }

                    // Update quantity score based on whether a match has been found
                    if (match != null) {
                        // Update node to save time on potential parent search
                        matches.put(goldNode, match);
                        // Update node uses to prevent node reuse
                        matches.put(match, goldNode);
                        // increment score sum
                        scoreSum += weight;

                        // Increment quantity (non-continuations)
                        quantityScoreSum++;
                        if (writeDetails) System.out.println("Scoring direct match to " + getPositionString(match));
                    } else {
                        if (writeDetails) System.out.println("No direct match.");
                        // Add to list of misses
                        misses.add(goldNode);
                    }
                }
            }
        }

        // Loop through bifurcation misses to determine if any might be a continuation
        if (testEnvironment) System.out.println("\nLooking for continuations");
        for (int i = 0; i < misses.size(); i++) {
            goldNode = (ParentedBinaryTreeNode) misses.get(i);
            goldData = goldNode.getSwcData();
            writeDetails = false;
            if (isWithinXYThreshold(xyCheck, goldData)) {
                writeDetails = true;
            }

            if (!goldNode.isLeaf()) {
                if (writeDetails || debug) System.out.println("\nTarget " + getPositionString(goldNode));

                // If a continuation match via (grand)parent and (grand)child was found
                if (isContinuation(goldNode, testNodeList)) {
                    // increment score sum
                    weight = this.isWeighted() ? ((Integer) weightMap.get(goldNode)).intValue() : 1;
                    scoreSum += weight;
                    if (writeDetails || debug) System.out.println("Found continuation");
                    // This node is now a continuation, not a miss
                    misses.remove(i);
                    i--;
                }
            }
        }

        // Calculate final scores
        if (weightSum > 0) {
            directMatchScore = quantityScoreSum / numberOfNodes;

            // Quality score is average weighted node quality, without accounting for excess nodes
            qualityScore = scoreSum / weightSum;

            if (testEnvironment) {
                System.out.println("WeightSum: " + weightSum);
                System.out.println("ScoreSum: " + scoreSum);
            }

            // Remove spurs prior to determining excess
            if (removeSpurs > 0) {
                removeSpurs(testRoot, removeSpurs);
            }
            // Excess nodes weighed by degree of excess terms without any matches in between
            weightSum += weighExcess(testRoot, goldTrees);
            finalScore = scoreSum / weightSum;
        }

    }

    /**
     * @param goldNode
     * @param testNodeList
     * @return ParentedBinaryTreeNode
     * Returns the best match to the target (gold) node
     */
    private ParentedBinaryTreeNode getClosestMatch(
            ParentedBinaryTreeNode goldNode, List goldNodeList, List testNodeList) {
        // Find nearest node to parent
        List nearbyNodesList = findNearestNodes(goldNode, testNodeList);
        ParentedBinaryTreeNode testNode, bestMatch = null;

        if (writeDetails) System.out.println(nearbyNodesList.size() + " nearby");
        // Check whether nearest node(s) has parent that matches one of gold node's parents
        List matchList = new ArrayList();
        for (int j = 0; j < nearbyNodesList.size(); j++) {
            testNode = (ParentedBinaryTreeNode) nearbyNodesList.get(j);
            // Check whether nodes' parents match (based on position and path length)
            if (writeDetails)
                System.out.println("Checking path lengths for match: " + getPositionString(testNode) + "; Dist: " + getDistance(goldNode, testNode));
            if (nodeMatches(goldNode, testNode, goldNodeList, testNodeList)) {
                matchList.add(testNode);
            }
        }

        if (matchList.size() == 1) {
            bestMatch = (ParentedBinaryTreeNode) matchList.get(0);
        } else if (matchList.size() > 1) {
            // Find the best match of those found based on descendant connectivity and/or proximity
            bestMatch = determineBestMatch(matchList, goldNode, testNodeList);
        }
        if (bestMatch != null && writeDetails)
            System.out.println("Best match is " + getPositionString(bestMatch));

        return bestMatch;
    }

    /**
     * @param matchList
     * @param goldNode
     * @param testNodeList
     * @return best ParentedBinaryTreeNode based on descendant connectivity and/or proximity
     */
    private ParentedBinaryTreeNode determineBestMatch(List matchList, ParentedBinaryTreeNode goldNode,
                                                      List testNodeList) {

        if (writeDetails) System.out.println("Determining best match");
        List confirmList = new ArrayList();
        ParentedBinaryTreeNode descendantNode, testDescendantNode, testNode, match;

        // For correction of test path length
        SwcDataNode goldData = goldNode.getSwcData();
        EuclideanPoint targetTrajectory, descendantTrajectory;
        Map descendantBranchLeft = new HashMap();
        Map trajectoryMap = new HashMap();

        if (goldNode.hasChildren()) {
            List nearbyList;

            // Path distances
            Map pathLengthMap = new HashMap();
            pathLengthMap.put(goldNode, new SwcSecondaryData());
            SwcSecondaryData parentPathLength, goldPathLength, testPathLength;
            double testXYPathLength, testZPathLength;

            // Euclidean distances
            List targetDistancesList = new ArrayList();
            SwcSecondaryData descendantDistances, targetDistances;
            for (int i = 0; i < matchList.size(); i++) {
                testNode = (ParentedBinaryTreeNode) matchList.get(i);
                targetDistances = new SwcSecondaryData();
                targetDistances.setXYPathLength(getXYDistance(goldNode, testNode));
                targetDistances.setZPathLength(getZDistance(goldNode, testNode));
                targetDistancesList.add(targetDistances);
            }

            // Traversing gold node descendants
            List currentList = new ArrayList(), nextList;
            currentList.add(goldNode.getParentedLeft());
            trajectoryMap.put(goldNode.getParentedLeft(), goldData.getSecondaryData().getLeftTrajectoryPoint());
            descendantBranchLeft.put(goldNode.getParentedLeft(), new Boolean(true));
            currentList.add(goldNode.getParentedRight());
            trajectoryMap.put(goldNode.getParentedRight(), goldData.getSecondaryData().getRightTrajectoryPoint());
            descendantBranchLeft.put(goldNode.getParentedRight(), new Boolean(false));

            while (currentList.size() > 0 && confirmList.size() == 0) {
                nextList = new ArrayList();

                // Loop through nodes at current branch order
                for (int i = 0; i < currentList.size(); i++) {
                    descendantNode = (ParentedBinaryTreeNode) currentList.get(i);
                    targetTrajectory = (EuclideanPoint) trajectoryMap.get(descendantNode);
                    // Must determine trajectory based on specific path
                    if (targetTrajectory.getX() == ReadSWC.TRAJECTORY_NONE || targetTrajectory.getZ() == ReadSWC.TRAJECTORY_NONE) {
                        EuclideanPoint tmpTrajectory = getTrajectoryForPath(goldNode, descendantNode);
                        if (targetTrajectory.getX() == ReadSWC.TRAJECTORY_NONE) {
                            targetTrajectory.setX(tmpTrajectory.getX());
                            targetTrajectory.setY(tmpTrajectory.getY());
                        }
                        if (targetTrajectory.getZ() == ReadSWC.TRAJECTORY_NONE) {
                            targetTrajectory.setZ(tmpTrajectory.getZ());
                        }
                    }

                    descendantTrajectory = descendantNode.getSwcData().getSecondaryData().getParentTrajectoryPoint();

                    // Add children to next list
                    if (descendantNode.hasChildren()) {
                        nextList.add(descendantNode.getLeft());
                        nextList.add(descendantNode.getRight());
                        trajectoryMap.put(descendantNode.getLeft(), targetTrajectory);
                        trajectoryMap.put(descendantNode.getRight(), targetTrajectory);
                    }

                    if (writeDetails)
                        System.out.println("Descendant node: " + descendantNode.getSwcData().getPositionString());

                    // Calculate path length from target node to current descendant
                    goldPathLength = new SwcSecondaryData();
                    goldPathLength.incrementPathLengths(descendantNode.getSwcData().getSecondaryData());
                    parentPathLength = (SwcSecondaryData) pathLengthMap.get(descendantNode.getParent());
                    goldPathLength.incrementPathLengths(parentPathLength);
                    pathLengthMap.put(descendantNode, goldPathLength);

                    // Find test nodes that match current descendant
                    nearbyList = findNearestNodes(descendantNode, testNodeList, true);

                    // Determine if any of these nodes is a descendant of a target node match
                    for (int j = 0; j < nearbyList.size(); j++) {
                        // testDescendant stays set as this, testNode climbs to look for match node
                        testDescendantNode = testNode = (ParentedBinaryTreeNode) nearbyList.get(j);
                        testPathLength = new SwcSecondaryData();

                        if (writeDetails)
                            System.out.println("Nearby descendant test node: " + testNode.getSwcData().getPositionString());

                        // Descendant euclidean distances
                        descendantDistances = new SwcSecondaryData();
                        descendantDistances.setXYPathLength(getXYDistance(descendantNode, testNode));
                        descendantDistances.setZPathLength(getZDistance(descendantNode, testNode));

                        // Climb matched descendant until a target match is found or the root is hit
                        while (testNode.getParent() != null) {
                            testPathLength.incrementPathLengths(testNode.getSwcData().getSecondaryData());

                            testNode = testNode.getParent();
                            // Check whether one of the match nodes is encountered while climbing
                            for (int k = 0; k < matchList.size(); k++) {
                                match = (ParentedBinaryTreeNode) matchList.get(k);
                                if (testNode == match) {
                                    if (writeDetails)
                                        System.out.println("Match (descendant's ancestor): " + getPositionString(match));
                                    targetDistances = (SwcSecondaryData) targetDistancesList.get(k);
                                    // Found a target match, need to check path length

                                    // Calculate test XY and Z path length modified by end node positions
                                    testXYPathLength = testPathLength.getXYPathLength()
                                            + getEndNodeXYDistanceDifference(goldData, targetTrajectory, testNode.getSwcData())
                                            + getEndNodeXYDistanceDifference(descendantNode.getSwcData(), descendantTrajectory, testDescendantNode.getSwcData());
                                    testZPathLength = testPathLength.getZPathLength()
                                            + getEndNodeZDistanceDifference(goldData, targetTrajectory, testNode.getSwcData())
                                            + getEndNodeZDistanceDifference(descendantNode.getSwcData(), descendantTrajectory, testDescendantNode.getSwcData());

                                    // If path lengths are within threshold, return this specific target match
                                    if (pathLengthMatches(goldPathLength, testXYPathLength, testZPathLength)) {
                                        confirmList.add(match);
                                        matchList.remove(k);
                                        k--;
                                    }
                                }
                            }
                        }
                    }
                }
                currentList = nextList;
            }
        }

        // Only one confirmed, so return it
        if (confirmList.size() == 1) {
            return (ParentedBinaryTreeNode) confirmList.get(0);
        }

        // If none of the nodes had a descendant match, or more than one confirmed, return closest node
        if (confirmList.size() == 0) {
            confirmList = matchList;
        }

        double distance, closestDistance = -1;
        ParentedBinaryTreeNode closestMatch = null;
        for (int j = 0; j < confirmList.size(); j++) {
            testNode = (ParentedBinaryTreeNode) confirmList.get(j);
            // Check whether nodes' parents match

            distance = getDistance(testNode, goldNode);
            if (closestDistance == -1 || distance < closestDistance) {
                closestDistance = distance;
                closestMatch = testNode;
            }
        }
        return closestMatch;

    }

    private ParentedBinaryTreeNode getNMPTerminationMatch(
            ParentedBinaryTreeNode goldNode, List goldNodeList, List testNodeList) {

        // Assemble termination node's ancestor path
        Set ancestors = new LinkedHashSet();
        ParentedBinaryTreeNode node = goldNode.getParent();
        do {
            ancestors.add(node);
            node = node.getParent();
        } while (node != null);

        // Find nearest node to parent
        List matchList = findNearestNMPTerminationMatches(goldNode, testNodeList);
        ParentedBinaryTreeNode testNode, bestMatch = null, initialNode;

        if (writeDetails) System.out.println(matchList.size() + " matches");
        // Check whether nearest node(s) has parent that matches one of gold node's parents

        // Remove any nodes in matchList that have an ancestor in the matchList
        for (int i = 0; i < matchList.size(); i++) {
            initialNode = (ParentedBinaryTreeNode) matchList.get(i);
            testNode = initialNode.getParent();
            while (testNode != null) {
                if (matchList.contains(testNode)) {
                    matchList.remove(initialNode);
                    i--;
                    break;
                }
                testNode = testNode.getParent();
            }
        }

        double percentDifference, lowestDifference = -1;
        // Find the closest remaining match
        for (int i = 0; i < matchList.size(); i++) {
            testNode = (ParentedBinaryTreeNode) matchList.get(i);

            // Attempt to match via normal means
            percentDifference = getMatchPathLengthDifference(goldNode, testNode, goldNodeList, testNodeList);

            // If no match, try matching accounting for rosettes
            if (percentDifference == 1) {
                percentDifference = getNMPTerminationMatchPathDifference(goldNode, testNode, goldNodeList, testNodeList);
            }

            // Check whether nodes' paths match
            if (percentDifference < 1) {
                if (writeDetails) System.out.println(getPositionString(testNode) + " matches");
                // If there are multiple matches, take the one with the smallest difference between path lengths
                if (lowestDifference == -1 || percentDifference < lowestDifference) {
                    lowestDifference = percentDifference;
                    bestMatch = testNode;
                }
            }
        }
        if (bestMatch != null && writeDetails)
            System.out.println("Closest is " + getPositionString(bestMatch)
                    + " with percent path length difference of " + lowestDifference);

        // Remove all nodes below the gold termination from test list
        if (bestMatch != null && bestMatch.hasChildren()) {
            LinkedStack stack = new LinkedStack();
            stack.push(bestMatch.getRight());
            stack.push(bestMatch.getLeft());
            while (!stack.isEmpty()) {
                testNode = (ParentedBinaryTreeNode) stack.pop();
                matches.put(testNode, goldNode);
                if (testNode.hasChildren()) {
                    stack.push(testNode.getLeft());
                    stack.push(testNode.getRight());
                }
            }
        }

        return bestMatch;
    }

    private List findNearestNMPTerminationMatches(
            ParentedBinaryTreeNode goldNode, List testNodeList) {
        List nearestNodeList = new ArrayList();
        ParentedBinaryTreeNode testNode, tmpNode;
        LinkedStack stack;
        boolean addNode;

        // Loop through test nodes
        for (int i = 0; i < testNodeList.size(); i++) {
            testNode = (ParentedBinaryTreeNode) testNodeList.get(i);
            // Gets distance between two nodes in space
            // If the distance is within the threshold, and this node hasn't been overused, add to list
            if (!matches.containsKey(testNode) && getDistance(goldNode, testNode) < ROSETTE_THRESHOLD) {
                if (writeDetails) {
                    SwcDataNode dat = testNode.getSwcData();
                    System.out.println("Found node: " + dat.getX() + "; " + dat.getY()
                            + "; XYDist: " + getXYDistance(goldNode, testNode)
                            + "; ZDist: " + getZDistance(goldNode, testNode));
                }
                addNode = true;
                if (testNode.hasChildren()) {
                    // To ensure this node is a rosette node, make sure it has no children outside the possible range of a rosette
                    stack = new LinkedStack();
                    stack.push(testNode.getRight());
                    stack.push(testNode.getLeft());
                    while (!stack.isEmpty()) {
                        tmpNode = (ParentedBinaryTreeNode) stack.pop();
                        if (getDistance(testNode, tmpNode) > ROSETTE_THRESHOLD) {
                            addNode = false;
                            break;
                        }
                    }
                }

                if (addNode) {
                    nearestNodeList.add(testNode);
                }
            }
        }
        return nearestNodeList;
    }

    /**
     * @param goldNode
     * @param testNode
     * @param goldNodeList
     * @param testNodeList
     * @return Path distance difference percentage if swc continuation is found within threshold of the gold termination
     * and the path lengths are within threshold, otherwise 1.
     */
    private double getNMPTerminationMatchPathDifference(
            ParentedBinaryTreeNode goldNode, ParentedBinaryTreeNode testNode,
            List goldNodeList, List testNodeList) {
        if (testNode.getParent() == null) {
            return -1;
        }

        // Find nearest continuation points in last span within threshold before the first parent bifurcation
        // Get treeNode based on test binary tree node
        SwcTreeNode treeNode = getNMPTreeNode(testNode);
        double runningPathLength = 0, selectedPathLength = 0, nodeDistance, smallestLocalDistance = -1;
        boolean found = false, wasOutsideThreshold = true;

        // Keep going until bifurcation is found
        do {
            // Determine distance between swc node and gold termination
            nodeDistance = SwcDataUtils.getDistance(treeNode.getSwcData(), goldNode.getSwcData());

            // If nodes are within threshold
            if (isWithinThreshold(treeNode.getSwcData(), goldNode.getSwcData())) {
                found = true;
                // If this node is the first in a span, reset smallest local distance
                if (wasOutsideThreshold || nodeDistance <= smallestLocalDistance) {
                    smallestLocalDistance = nodeDistance;
                    selectedPathLength = runningPathLength;
                }
                wasOutsideThreshold = false;
            }
            // Outside of threshold range, smallestLocalDistance = -1 as a flag
            else {
                wasOutsideThreshold = true;
            }

            runningPathLength += SwcDataUtils.getDistance(treeNode.getSwcData(), treeNode.getSwcParent().getSwcData());
            treeNode = treeNode.getSwcParent();
        } while (treeNode.getChildren().size() < 2);

        // If no continuation was within threshold, return the failure value
        if (!found) {
            return 1;
        }

        // Get distance between nodes to use in path length check
        double difference, percentDifference;

        // Get path length at each node
        SwcDataNode goldData = (SwcDataNode) goldNode.getData();
        double goldPathLength = goldData.getSecondaryData().getPathLength();
        SwcDataNode testData = (SwcDataNode) testNode.getData();
        // Get test path length, accounting for distance from node to matched continuation point
        double testPathLength = testData.getSecondaryData().getPathLength() - selectedPathLength;

        // Start with parents of original nodes
        goldNode = goldNode.getParent();
        testNode = testNode.getParent();

        Set checkedNodes = new HashSet();
        List nearbyNodes;
        boolean notDone = true;
        while (notDone) {
            if (writeDetails) {
                System.out.println("Looking at " + getPositionString(goldNode) + " and " + getPositionString(testNode));
                System.out.println("Baseline path length: " + goldPathLength);
                System.out.println("Sample path length: " + testPathLength);
            }
            // If ancestors are within euclidean distance threshold, see if path length matches
            if (isWithinThreshold(goldNode, testNode)) {
                // Find path length difference
                difference = Math.abs(goldPathLength - testPathLength);
                // Subtract out error (distance between ancestor nodes)
                difference -= getDistance(goldNode, testNode);
                // Not factoring distance between continuation and gold termination

                percentDifference = difference / goldPathLength;
                if (writeDetails) System.out.println("Difference: " + percentDifference);
                if (percentDifference < XYPathErrorThreshold) {
                    return percentDifference;
                }
                // Node position matches, but path length doesn't. This is a miss
                else {
                    return 1;
                }
                // Otherwise keep trying, as the "match" by location may have been wrong
            } else {
                // Decide which node to traverse up next
                if (goldPathLength < testPathLength) {
                    // If at root OR if ancestor has a known match and it hasn't been tried, then this node is a miss
					/*if (goldNode.getParent() == null ||
							(matches.containsKey(goldNode) && testNode != matches.get(goldNode))){*/
                    // If at root, done (not stopping in other cases. false positives better than false negatives)
                    if (goldNode.getParent() == null) {
                        notDone = false;
                    } else {
                        // Before moving on, check whether node matches a previously tested node
                        nearbyNodes = findNearestNodes(goldNode, testNodeList, false);
                        for (int i = 0; i < nearbyNodes.size(); i++) {
                            if (checkedNodes.contains(nearbyNodes.get(i))) {
                                // Node match found, path length by definition has not checked out
                                notDone = false;
                                break;
                            }
                        }

                        // Add node to checked nodes
                        checkedNodes.add(goldNode);
                        // Update path length
                        goldPathLength += ((SwcDataNode) goldNode.getData()).getSecondaryData().getPathLength();
                        // Climb to next parent
                        goldNode = goldNode.getParent();
                    }
                }
                // Traversing up test/test path
                else {
                    // If at root OR if ancestor has a known match and it hasn't been tried, then this node is a miss
                    if (testNode.getParent() == null) {
                        notDone = false;
                    } else {
                        // Before moving on, check whether node matches a previously tested node
                        nearbyNodes = findNearestNodes(testNode, goldNodeList, false);
                        for (int i = 0; i < nearbyNodes.size(); i++) {
                            if (checkedNodes.contains(nearbyNodes.get(i))) {
                                // Node match found, path length by definition has not checked out
                                notDone = false;
                                break;
                            }
                        }

                        // Add node to checked nodes
                        checkedNodes.add(testNode);
                        // Update path length
                        testPathLength += ((SwcDataNode) testNode.getData()).getSecondaryData().getPathLength();
                        // Climb to next parent
                        testNode = testNode.getParent();
                    }
                }
            }
        }

        // No match
        return 1;
    }

    private SwcTreeNode getNMPTreeNode(ParentedBinaryTreeNode node) {
        return (SwcTreeNode) testTreePathNodes.get(new Integer(node.getSwcData().getNodeId()));
    }

    /**
     * @param goldNode
     * @param testNodeList
     * @return Whether or not a node is a continuation in the test tree
     * Find first parent known to be in test tree.
     * Search down from gold node for matches (within dist threshold), then compare path lengths
     */
    private boolean isContinuation(ParentedBinaryTreeNode goldNode, List testNodeList) {
        return isContinuation(goldNode, testNodeList, true);
    }

    private boolean isContinuation(ParentedBinaryTreeNode goldNode, List testNodeList, boolean addToLists) {
        // Terminal nodes can't be continuations
        if (!goldNode.hasChildren()) {
            return false;
        }
        List ancestorNodeMatches;

        // Get path length at each node
        SwcSecondaryData goldPathLength = new SwcSecondaryData();

        // Find first parent of gold node that has a match
        ParentedBinaryTreeNode ancestorMatch = null, ancestorNode = goldNode.getParent();
        goldPathLength.setPathLength(goldNode.getSwcData().getSecondaryData().getPathLength());
        goldPathLength.setXYPathLength(goldNode.getSwcData().getSecondaryData().getXYPathLength());
        goldPathLength.setZPathLength(goldNode.getSwcData().getSecondaryData().getZPathLength());
        boolean isMatch = false, isBranchLeft = goldNode.isLeft();

        // Look for match until one is found or root is reached
        while (ancestorNode != null) {
            // If known match, use it (and don't keep looking)
            if (matches.containsKey(ancestorNode)) {
                if (writeDetails) System.out.println("Known ancestor node");
                ancestorMatch = (ParentedBinaryTreeNode) matches.get(ancestorNode);
                return isContinuation(goldNode, ancestorNode, ancestorMatch, isBranchLeft,
                        goldPathLength, testNodeList, addToLists);
            }
            // While no match was made, that might just be because the node's connection to its parent was wrong
            ancestorNodeMatches = findNearestNodes(ancestorNode, testNodeList);
            for (int i = 0; i < ancestorNodeMatches.size() && !isMatch; i++) {
                ancestorMatch = (ParentedBinaryTreeNode) ancestorNodeMatches.get(i);
                isMatch = isContinuation(goldNode, ancestorNode, ancestorMatch, isBranchLeft,
                        goldPathLength, testNodeList, addToLists);
                // If we've found the match, let's get out of here
                if (isMatch) return true;
            }

            // No match, keep going
            goldPathLength.incrementPathLength(ancestorNode.getSwcData().getSecondaryData().getPathLength());
            goldPathLength.incrementXYPathLength(ancestorNode.getSwcData().getSecondaryData().getXYPathLength());
            goldPathLength.incrementZPathLength(ancestorNode.getSwcData().getSecondaryData().getZPathLength());
            isBranchLeft = ancestorNode.isLeft();
            ancestorNode = ancestorNode.getParent();
        }

        // Found no match, so this isn't a continuation
        return false;
    }

    // Subroutine for overall isContinuation function
    private boolean isContinuation(ParentedBinaryTreeNode goldNode, ParentedBinaryTreeNode ancestorNode,
                                   ParentedBinaryTreeNode ancestorMatch, boolean isLeftBranch, SwcSecondaryData goldPathLength,
                                   List testNodeList, boolean addToLists) {

        // If there is no parent match, this cannot be confirmed as a continuation
        if (ancestorMatch == null) return false;

        if (writeDetails)
            System.out.println("Ancestor Match, Gold: " + getPositionString(ancestorNode) + "; test: " + getPositionString(ancestorMatch));

        Map pathLengthMap = new HashMap();
        pathLengthMap.put(goldNode, goldPathLength);

        ParentedBinaryTreeNode leftChildMatch, rightChildMatch;
        EuclideanPoint ancestorTrajectory;
        if (isLeftBranch) {
            ancestorTrajectory = ancestorNode.getSwcData().getSecondaryData().getLeftTrajectoryPoint();
        } else {
            ancestorTrajectory = ancestorNode.getSwcData().getSecondaryData().getRightTrajectoryPoint();
        }

        // Search down either side from gold node for match(es)
        leftChildMatch = getDescendantInForContinuation(
                goldNode.getParentedLeft(), ancestorNode, ancestorMatch, ancestorTrajectory, pathLengthMap, testNodeList);
        rightChildMatch = getDescendantInForContinuation(
                goldNode.getParentedRight(), ancestorNode, ancestorMatch, ancestorTrajectory, pathLengthMap, testNodeList);

        // If both are found, then there is a common node outside of the threshold range
        if (leftChildMatch != null && rightChildMatch != null) {
            ParentedBinaryTreeNode commonAncestor = leastCommonAncestor(leftChildMatch, rightChildMatch, ancestorMatch);
            // TODO: Decide whether this is an appropriate determinant
            if (commonAncestor != null && isWithinDistantMatchThreshold(commonAncestor, goldNode)) {
                matches.put(goldNode, commonAncestor);
                matches.put(commonAncestor, goldNode);
                if (writeDetails)
                    System.out.println("Distant match to " + getPositionString(commonAncestor));
                if (addToLists) {
                    distantMatches.add(goldNode);
                }
            } else if (addToLists) {
                continuations.add(goldNode);
            }
            return true;
        }
        // Return true if either child's side gets a match
        else if (leftChildMatch != null || rightChildMatch != null) {
            if (addToLists) {
                continuations.add(goldNode);
            }
            return true;
        }
        return false;
    }

    private ParentedBinaryTreeNode getDescendantInForContinuation(
            ParentedBinaryTreeNode firstNode, ParentedBinaryTreeNode ancestorNode,
            ParentedBinaryTreeNode ancestorMatch, EuclideanPoint ancestorTrajectory,
            Map pathLengthMap, List testNodeList) {
        ParentedBinaryTreeNode childMatch = null, tmpNode, goldNode;
        double testXYPathLength, testZPathLength;
        boolean done;
        SwcSecondaryData goldPathLength, prevPathLength, testPathLength;
        EuclideanPoint specificAncestorTrajectory = ancestorTrajectory, descendantTrajectory;
        List testMatches;

        LinkedStack stack = new LinkedStack();
        stack.push(firstNode);
        // Search down from gold node for matches
        while (!stack.isEmpty()) {
            goldNode = (ParentedBinaryTreeNode) stack.pop();
            if (writeDetails) System.out.println("Descendant " + getPositionString(goldNode.getSwcData()));
            descendantTrajectory = goldNode.getSwcData().getSecondaryData().getParentTrajectoryPoint();

            // If there is a known match for the descendant, use it
            if (matches.containsKey(goldNode)) {
                testMatches = new ArrayList();
                testMatches.add(matches.get(goldNode));
            } else {
                // Find nodes within threshold of descendant
                testMatches = findNearestNodes(goldNode, testNodeList, false);
            }

            // Calculate and score gold path length
            prevPathLength = (SwcSecondaryData) pathLengthMap.get(goldNode.getParent());
            goldPathLength = new SwcSecondaryData();
            goldPathLength.setPathLength(
                    goldNode.getSwcData().getSecondaryData().getPathLength()
                            + prevPathLength.getPathLength());
            goldPathLength.setXYPathLength(
                    goldNode.getSwcData().getSecondaryData().getXYPathLength()
                            + prevPathLength.getXYPathLength());
            goldPathLength.setZPathLength(
                    goldNode.getSwcData().getSecondaryData().getZPathLength()
                            + prevPathLength.getZPathLength());
            pathLengthMap.put(goldNode, goldPathLength);

            for (int i = 0; i < testMatches.size(); i++) {
                childMatch = (ParentedBinaryTreeNode) testMatches.get(i);
                if (writeDetails)
                    System.out.println("Matching on gold child: " + getPositionString(goldNode) + "; test match: " + getPositionString(childMatch));

                // Go up to parent (or node with path length calculated) to find path length
                testPathLength = new SwcSecondaryData();
                testPathLength.addData(childMatch.getSwcData().getSecondaryData());
                tmpNode = childMatch.getParent();

                // Don't bother tracing up if child match's parent is ancestor
                done = (tmpNode == ancestorMatch);
                while (!done) {
                    if (pathLengthMap.containsKey(tmpNode)) {
                        prevPathLength = (SwcSecondaryData) pathLengthMap.get(tmpNode);
                        testPathLength.incrementPathLengths(prevPathLength);
                        done = true;
                    } else {
                        prevPathLength = (SwcSecondaryData) pathLengthMap.get(tmpNode);
                        testPathLength.incrementPathLengths(tmpNode.getSwcData().getSecondaryData());
                        tmpNode = tmpNode.getParent();
                        if (tmpNode == null) {
                            // Path not found: matched to descendant was ancestor of matched ancestor
                            done = true;
                        }
                    }
                    if (tmpNode == ancestorMatch) {
                        done = true;
                    }
                }

                // Only continue if the descendant match is the descendant of the ancestor match
                if (tmpNode == null) {
                    if (writeDetails) System.out.println("Descendant match not a descendant of ancestor match");
                } else {
                    pathLengthMap.put(childMatch, testPathLength);
                    if (ancestorTrajectory.getX() == ReadSWC.TRAJECTORY_NONE || ancestorTrajectory.getZ() == ReadSWC.TRAJECTORY_NONE) {
                        specificAncestorTrajectory = getTrajectoryForPath(ancestorNode, goldNode);
                        if (ancestorTrajectory.getX() != ReadSWC.TRAJECTORY_NONE) {
                            specificAncestorTrajectory.setX(ancestorTrajectory.getX());
                            specificAncestorTrajectory.setY(ancestorTrajectory.getY());
                        }
                        if (ancestorTrajectory.getZ() != ReadSWC.TRAJECTORY_NONE) {
                            specificAncestorTrajectory.setZ(ancestorTrajectory.getZ());
                        }
                    }

                    if (writeDetails)
                        System.out.println("Path distances: " + goldPathLength.getPathLength() + " " + testPathLength.getPathLength());

                    // Calculate test XY and Z path length modified by end node positions
                    testXYPathLength = testPathLength.getXYPathLength()
                            + getEndNodeXYDistanceDifference(ancestorNode.getSwcData(), specificAncestorTrajectory, ancestorMatch.getSwcData())
                            + getEndNodeXYDistanceDifference(goldNode.getSwcData(), descendantTrajectory, childMatch.getSwcData());
                    testZPathLength = testPathLength.getZPathLength()
                            + getEndNodeZDistanceDifference(ancestorNode.getSwcData(), specificAncestorTrajectory, ancestorMatch.getSwcData())
                            + getEndNodeZDistanceDifference(goldNode.getSwcData(), descendantTrajectory, childMatch.getSwcData());

                    if (writeDetails)
                        System.out.println("XY Test Path: " + testPathLength.getXYPathLength() + "; ancestorAdjust: " +
                                getEndNodeXYDistanceDifference(ancestorNode.getSwcData(), specificAncestorTrajectory, ancestorMatch.getSwcData()) +
                                "; descendantAdjust: " + getEndNodeXYDistanceDifference(goldNode.getSwcData(), descendantTrajectory, childMatch.getSwcData()));

                    if (pathLengthMatches(goldPathLength, testXYPathLength, testZPathLength)) {
                        if (writeDetails)
                            System.out.println("Found child match for continuation: " + childMatch.getData());
                        return childMatch;
                    }
                    // Otherwise keep trying, as the "match" by location may have been wrong
                    else if (writeDetails) System.out.println("Try another.");
                }
            }
            // Add child nodes to stack only if this node didn't already have a known match
            if (goldNode.hasChildren() && !matches.containsKey(goldNode)) {
                stack.push(goldNode.getParentedLeft());
                stack.push(goldNode.getParentedRight());
            }
        }
        return null;
    }

    private boolean pathLengthMatches(SwcSecondaryData goldPathLength,
                                      double xyTestPathLength, double zTestPathLength) {

        double xyLocalPathErrorThreshold = XYPathErrorThreshold,
                zLocalPathErrorThreshold = ZPathErrorThreshold;

        double xyDiff = Math.abs(goldPathLength.getXYPathLength() - xyTestPathLength);
        double xyErr = xyDiff / goldPathLength.getPathLength();
        if (writeDetails)
            System.out.println("goldXYPath: " + goldPathLength.getXYPathLength() + "; testXYPath: " + xyTestPathLength);
        if (writeDetails) System.out.println("xyDiff = " + xyDiff + "; xyErr: " + xyErr);

        double zDiff = Math.abs(goldPathLength.getZPathLength() - zTestPathLength);
        double zErr = zDiff / goldPathLength.getPathLength();
        if (writeDetails)
            System.out.println("goldZPath: " + goldPathLength.getZPathLength() + "; testZPath: " + zTestPathLength);
        if (writeDetails) System.out.println("zDiff = " + zDiff + "; zErr = " + zErr);

        if (goldPathLength.getXYPathLength() < XYThreshold) {
            // If both paths are within the euclidean threshold distance, no check needed
            if (xyTestPathLength < XYThreshold) {
                xyErr = 0;
            }
            // If gold standard path is within, but test path is not, increase the path length error threshold
            else {
                xyLocalPathErrorThreshold = LocalPathErrorThreshold;
            }
        }
        if (goldPathLength.getZPathLength() < ZThreshold) {
            // If both paths are within the euclidean threshold distance, no check needed
            if (zTestPathLength < ZThreshold) {
                zErr = 0;
            }
            // If gold standard path is within, but test path is not, increase the path length error threshold
            else {
                zLocalPathErrorThreshold = LocalPathErrorThreshold;
            }
        }

        return (xyErr < xyLocalPathErrorThreshold && zErr < zLocalPathErrorThreshold);
    }

    private ParentedBinaryTreeNode leastCommonAncestor(
            ParentedBinaryTreeNode node1, ParentedBinaryTreeNode node2, ParentedBinaryTreeNode knownCommonAncestor) {
        List node1List = new ArrayList();

        if (writeDetails) {
            System.out.println("Known common ancestor: " + getPositionString(knownCommonAncestor));
        }
        // Assemble node1's ancestor list
        node1 = node1.getParent();
        while (node1 != knownCommonAncestor) {
            if (writeDetails) System.out.println("Node1: " + getPositionString(node1));
            node1List.add(node1);
            node1 = node1.getParent();
        }

        // Find first ancestor node of node2 that is an ancestor node of node1
        node1 = node2.getParent();
        while (node2 != knownCommonAncestor && node2 != null) {
            if (node1List.contains(node2)) {
                return node2;
            }
            node2 = node2.getParent();
        }

        return null;
    }

    /**
     * @param goldNode
     * @param testNodeList
     * @return list of nearest nodes
     */
    private List findNearestNodes(BinaryTreeNode goldNode, List testNodeList) {
        return findNearestNodes(goldNode, testNodeList, true);
    }

    private List findNearestNodes(BinaryTreeNode goldNode, List testNodeList, boolean checkPreviousUse) {
        List nearestNodeList = new ArrayList();
        ParentedBinaryTreeNode testNode;

        // Loop through test nodes
        for (int i = 0; i < testNodeList.size(); i++) {
            testNode = (ParentedBinaryTreeNode) testNodeList.get(i);
            // Gets distance between two nodes in space
            // If the distance is within the threshold, and this node hasn't been overused, add to list
            if (((!checkPreviousUse || !matches.containsKey(testNode))) && isWithinThreshold(goldNode, testNode)) {
                if (writeDetails) {
                    SwcDataNode dat = (SwcDataNode) testNode.getData();
                    System.out.println("Found node: " + dat.getX() + "; " + dat.getY()
                            + "; XYDist: " + getXYDistance(goldNode, testNode)
                            + "; ZDist: " + getZDistance(goldNode, testNode));
                }
                nearestNodeList.add(testNode);
            }
        }

        return nearestNodeList;
    }

    // Determine whether the node is a match or not
    private boolean nodeMatches(ParentedBinaryTreeNode goldNode, ParentedBinaryTreeNode testNode,
                                List goldNodeList, List testNodeList) {
        double lengthDiff = getMatchPathLengthDifference(goldNode, testNode, goldNodeList, testNodeList);
        if (lengthDiff < 1)
            return true;
        else
            return false;
    }

    /**
     * @param goldNode
     * @param testNode
     * @param goldNodeList
     * @param testNodeList
     * @return Either the path length percent difference (XY and Z included) if XY and Z length error rates are within threshold,
     * or 1 if they are not within threshold
     */
    private double getMatchPathLengthDifference(ParentedBinaryTreeNode goldNode, ParentedBinaryTreeNode testNode,
                                                List goldNodeList, List testNodeList) {
        if (testNode.getParent() == null) {
            return 1;
        }

        // Used for determining path length threshold based on relative amounts of XY vs Z error
        ParentedBinaryTreeNode goldTarget = goldNode;

        // Get path length at each node
        SwcSecondaryData goldPathLength = new SwcSecondaryData();
        goldPathLength.incrementPathLengths(goldNode.getSwcData().getSecondaryData());
        SwcSecondaryData testPathLength = new SwcSecondaryData();
        testPathLength.incrementPathLengths(testNode.getSwcData().getSecondaryData());

        SwcDataNode goldData;
        EuclideanPoint ancestorTrajectory;
        double testXYPathLength, testZPathLength;

        // Calculate difference in path length for test path based on distance from gold node
        double testPathXYMod = getEndNodeXYDistanceDifference(
                goldNode.getSwcData(), goldNode.getSwcData().getSecondaryData().getParentTrajectoryPoint(), testNode.getSwcData());
        // Since this path modification will persist for all ancestor checks, it can be stored directly in the continually updated path length
        testPathLength.incrementXYPathLength(testPathXYMod);
        double testPathZMod = getEndNodeZDistanceDifference(
                goldNode.getSwcData(), goldNode.getSwcData().getSecondaryData().getParentTrajectoryPoint(), testNode.getSwcData());
        testPathLength.incrementZPathLength(testPathZMod);

        if (writeDetails)
            System.out.println("Test Path Mods (XY,Z): (" + testPathXYMod + ", " + testPathZMod + "); TrajecoryPt: " +
                    getPositionString(goldNode.getSwcData().getSecondaryData().getParentTrajectoryPoint()));

        // Start with parents of original nodes, climb using goldNode and testNode
        boolean isBranchLeft = goldNode.isLeft();
        goldNode = goldNode.getParent();
        testNode = testNode.getParent();

        Set checkedNodes = new HashSet();
        List nearbyNodes;
        boolean noMatch = true, notDone = true;
        while (noMatch && notDone) {
            if (writeDetails || debug) {
                System.out.println("Looking at ancestor " + getPositionString(goldNode) + " and " + getPositionString(testNode));
            }
            // If the gold standard and test ancestors are within threshold distance, confirm match by path length
            if (isWithinThreshold(goldNode, testNode)) {
                goldData = goldNode.getSwcData();
                // Get the ancestor trajectory for modifying test path length
                if (isBranchLeft) {
                    ancestorTrajectory = goldData.getSecondaryData().getLeftTrajectoryPoint();
                } else {
                    ancestorTrajectory = goldData.getSecondaryData().getRightTrajectoryPoint();
                }
                // If the ancestor has a child within threshold, a path specific trajectory must be calculated
                if (ancestorTrajectory.getX() == ReadSWC.TRAJECTORY_NONE || ancestorTrajectory.getZ() == ReadSWC.TRAJECTORY_NONE) {
                    EuclideanPoint tmpTrajectory = getTrajectoryForPath(goldNode, goldTarget);
                    if (ancestorTrajectory.getX() == ReadSWC.TRAJECTORY_NONE) {
                        ancestorTrajectory.setX(tmpTrajectory.getX());
                        ancestorTrajectory.setY(tmpTrajectory.getY());
                    }
                    if (ancestorTrajectory.getZ() == ReadSWC.TRAJECTORY_NONE) {
                        ancestorTrajectory.setZ(tmpTrajectory.getZ());
                    }
                    if (writeDetails)
                        System.out.println("Getting trajectory for path with ancestor: " + getPositionString(goldNode) + "; descendant: " + getPositionString(goldTarget) + ": " + getPositionString(ancestorTrajectory));
                }

                if (writeDetails)
                    System.out.println("Gold path length: " + goldPathLength.getPathLength() + "; Test path length: " + testPathLength.getPathLength()
                            + "; TrajectoryPt: " + ancestorTrajectory);

                // Calculate test XY and Z path length modified by end node positions
                // tempoary variables used because this modification will change with each successive ancestor
                testXYPathLength = testPathLength.getXYPathLength()
                        + getEndNodeXYDistanceDifference(goldData, ancestorTrajectory, testNode.getSwcData());
                testZPathLength = testPathLength.getZPathLength()
                        + getEndNodeZDistanceDifference(goldData, ancestorTrajectory, testNode.getSwcData());

                if (pathLengthMatches(goldPathLength, testXYPathLength, testZPathLength)) {
                    // Only used by NMP, so just return xyError
                    return Math.abs(goldPathLength.getXYPathLength() - testXYPathLength) / goldPathLength.getPathLength();
                } else {
                    return 1;
                }
                // Otherwise keep trying, as the "match" by location may have been wrong
            } else {
                // Decide which node to traverse up next
                if (goldPathLength.getPathLength() < testPathLength.getPathLength()) {
                    // If at root OR if ancestor has a known match and it hasn't been tried, then this node is a miss
					/*if (goldNode.getParent() == null ||
							(matches.containsKey(goldNode) && testNode != matches.get(goldNode))){*/
                    // If at root, done (not stopping in other cases. false positives better than false negatives)
                    if (goldNode.getParent() == null) {
                        notDone = false;
                    } else {
                        // Before moving on, check whether node matches a previously tested node
                        nearbyNodes = findNearestNodes(goldNode, testNodeList, false);
                        for (int i = 0; i < nearbyNodes.size(); i++) {
                            if (checkedNodes.contains(nearbyNodes.get(i))) {
                                // Node match found, path length by definition has not checked out
                                notDone = false;
                                break;
                            }
                        }

                        // Add node to checked nodes
                        checkedNodes.add(goldNode);
                        // Update path length
                        goldPathLength.incrementPathLengths(goldNode.getSwcData().getSecondaryData());
                        // Climb to next parent
                        isBranchLeft = goldNode.isLeft();
                        goldNode = goldNode.getParent();
                    }
                } else {
                    // If at root OR if ancestor has a known match and it hasn't been tried, then this node is a miss
                    if (testNode.getParent() == null) {
                        notDone = false;
                    } else {
                        // Before moving on, check whether node matches a previously tested node
                        nearbyNodes = findNearestNodes(testNode, goldNodeList, false);
                        for (int i = 0; i < nearbyNodes.size(); i++) {
                            if (checkedNodes.contains(nearbyNodes.get(i))) {
                                // Node match found, path length by definition has not checked out
                                notDone = false;
                                break;
                            }
                        }

                        // Add node to checked nodes
                        checkedNodes.add(testNode);
                        // Update path length
                        testPathLength.incrementPathLengths(testNode.getSwcData().getSecondaryData());
                        // Climb to next parent
                        testNode = testNode.getParent();
                    }
                }
            }
        }

        // No match, return 1
        return 1;
    }

    // Excess nodes weighed by degree of excess terms without any matches in between
    private double weighExcess(SwcTreeNode testRoot, List goldTrees) {
        int weightSum = 0;
        // Traverse gold tree and get a list of nodes
        List goldNodeList = new ArrayList();
        ParentedBinaryTreeNode rootNode;
        for (int i = 0; i < goldTrees.size(); i++) {
            rootNode = (ParentedBinaryTreeNode) goldTrees.get(i);
            goldNodeList.addAll(
                    BinaryTreeUtils.createNodeList(rootNode));
        }

        ParentedBinaryTreeNode node;
        // Get all nodes into stack such that nodes will always pop before their parents
        LinkedStack setupStack, useStack = new LinkedStack();
        for (int i = 0; i < testRoot.getChildren().size(); i++) {
            node = (ParentedBinaryTreeNode) testRoot.getChildren().get(i);
            setupStack = new LinkedStack();
            setupStack.push(node);
            while (!setupStack.isEmpty()) {
                node = (ParentedBinaryTreeNode) setupStack.pop();
                if (node.hasChildren()) {
                    setupStack.push(node.getParentedLeft());
                    setupStack.push(node.getParentedRight());
                    // Purposefully not including root node
                    useStack.push(node.getParentedLeft());
                    useStack.push(node.getParentedRight());
                }
            }
        }

        List nearbyNodes;
        int excess;
        Map directTermExcess = new HashMap();
        SwcDataNode data;

        // Determine whether test nodes are excess or not
        while (!useStack.isEmpty()) {
            node = (ParentedBinaryTreeNode) useStack.pop();
            data = node.getSwcData();
            writeDetails = false;
            if (isWithinXYThreshold(xyCheck, data)) {
                writeDetails = true;
                System.out.println("Excess Node Check X: " + data.getX() + "; Y: " + data.getY());
            }
            // Bifurcation node
            if (node.hasChildren()) {
                if (writeDetails) System.out.println("Has children");
                // Accumulating terminal hits under this node
                excess = ((Integer) directTermExcess.get(node.getLeft())).intValue();
                excess += ((Integer) directTermExcess.get(node.getRight())).intValue();
				
				/* Only add to the weight if this node is a miss, not a  
				   continuation, and if there are no unmatched nearby nodes */
                if (!matches.containsKey(node) && findNearestNodes(node, goldNodeList).size() == 0
                        && !isContinuation(node, goldNodeList, false)) {
                    if (writeDetails) System.out.println("Is Excess");
                    excessNodes.put(node, new Integer(excess));
                    // don't add to the weight if this node is the parent of a spur
                    if (!spurList.contains(node)) {
                        weightSum += excess;
                    }
                } else {
                    if (writeDetails) System.out.println("Not Excess");
                    excess = 0;
                }
            }
            // Terminal node
            else {
                if (writeDetails) System.out.println("Is Termination");
                excess = 0;
                // Determine whether terminal node is excess (miss)
                // Excess if not a spur, no match, no match to parent, and no non-matched node within threshold
                if (!spurList.contains(node) && !matches.containsKey(node) && !matches.containsKey(node.getParent())
                        && findNearestNodes(node, goldNodeList).size() == 0) {
                    // See if an unselected gold node matches
                    nearbyNodes = findNearestNodes(node, goldNodeList);
                    if (nearbyNodes.size() > 0) {
                        // Remove nearest node so it cannot be used again
                        removeNearestNode(node, nearbyNodes, goldNodeList);
                    } else {
                        if (writeDetails) System.out.println("Is Excess");
                        excess = 1;
                        excessNodes.put(node, new Integer(1));
                        weightSum += excess;
                    }
                }
            }
            // Save excess nodes for display at end
            directTermExcess.put(node, new Integer(excess));
        }

        return weightSum;
    }

    private void removeNearestNode(ParentedBinaryTreeNode node, List nearbyNodes, List goldNodeList) {
        double distance, closestDistance = -1;
        ParentedBinaryTreeNode closestMatch = null, nearbyNode;
        for (int j = 0; j < nearbyNodes.size(); j++) {
            nearbyNode = (ParentedBinaryTreeNode) nearbyNodes.get(j);
            // Check whether nodes' parents match

            distance = getDistance(node, nearbyNode);
            if (closestDistance == -1 || distance < closestDistance) {
                closestDistance = distance;
                closestMatch = nearbyNode;
            }
        }
        goldNodeList.remove(closestMatch);
    }

    private void generateNodeWeights(SwcTreeNode root) {
        // load up weightMap by degree minus all terminal spurs
        ParentedBinaryTreeNode binaryRoot, node;
        LinkedStack initStack, mainStack;
        int weight;
        for (int i = 0; i < root.getChildren().size(); i++) {
            binaryRoot = (ParentedBinaryTreeNode) root.getChildren().get(i);
            initStack = new LinkedStack();
            initStack.push(binaryRoot);
            mainStack = new LinkedStack();
            mainStack.push(binaryRoot);
            // Add all nodes to the main stack
            while (!initStack.isEmpty()) {
                node = (ParentedBinaryTreeNode) initStack.pop();
                if (node.hasChildren()) {
                    initStack.push(node.getParentedLeft());
                    initStack.push(node.getParentedRight());
                    mainStack.push(node.getParentedLeft());
                    mainStack.push(node.getParentedRight());
                }
            }

            if (testEnvironment) System.out.println("Generating node weights");

            // Check for spurs using the mainStack, from terminals to root
            while (!mainStack.isEmpty()) {
                node = (ParentedBinaryTreeNode) mainStack.pop();

                writeDetails = isWithinXYThreshold(xyCheck, node.getSwcData());
                if (writeDetails || debug) {
                    System.out.println("Determining weight for " + node.getSwcData());
                }
                if (node.isLeaf()) {
                    if (writeDetails || debug) System.out.println("  Is leaf");
                    weightMap.put(node, new Integer(1));
                } else {
                    weight = 0;
                    if (node.getLeft().isLeaf() && node.getRight().isLeaf()) {
                        if (spurList.contains(node.getLeft())
                                || spurList.contains(node.getRight())) {
                            // If one child is spur and other not, this node is a continuation,
                            // but its weight still needs to be passed up to parent
                            if (writeDetails || debug) System.out.println("  At least one leaf child is spur");
                            weight = 1;
                        } else {
                            if (writeDetails || debug) System.out.println("  Both leaf children, neither spur");
                            weight = 2;
                        }
                    } else if (spurList.contains(node)) {
                        if (writeDetails || debug) System.out.println("  One child bif, one spur leaf");
                        // One spur leaf, one bifurcation
                        if (node.getLeft().isLeaf()) {
                            // Right node is bifurcation with weight, pass it upwards
                            weight += ((Integer) weightMap.get(node.getRight())).intValue();
                        } else {
                            // Left node is bifurcation with weight, pass it upwards
                            weight += ((Integer) weightMap.get(node.getLeft())).intValue();
                        }
                    } else {
                        if (writeDetails || debug) System.out.println("  Both children bifurcations");
                        weight += ((Integer) weightMap.get(node.getLeft())).intValue();
                        weight += ((Integer) weightMap.get(node.getRight())).intValue();
                    }
                    weightMap.put(node, new Integer(weight));
                }
            }
        }
    }

    private void removeSpurs(SwcTreeNode root, double threshold) {
        List children = new ArrayList();
        ParentedBinaryTreeNode binaryRoot;
        for (int i = 0; i < root.getChildren().size(); i++) {
            binaryRoot = (ParentedBinaryTreeNode) root.getChildren().get(i);
            binaryRoot = removeSpurs(binaryRoot, threshold);
            children.add(binaryRoot);
        }
        root.setChildren(children);
    }

    private ParentedBinaryTreeNode removeSpurs(
            ParentedBinaryTreeNode root, double threshold) {
        ParentedBinaryTreeNode node, newNode;
        spurList = new ArrayList();
        double distance;
        boolean bothChildrenAreSpurs;

        if (testEnvironment) System.out.println("Removing Spurs");

        LinkedStack stack = new LinkedStack();
        stack.push(root);
        while (!stack.isEmpty()) {
            node = (ParentedBinaryTreeNode) stack.pop();
            if (isWithinXYThreshold(xyCheck, node.getSwcData()))
                System.out.println("Spur check on " + getPositionString(node));
            if (node.hasChildren()) {
                stack.push(node.getParentedLeft());
                stack.push(node.getParentedRight());
            } else {
                // Node is termination: determine if spur
                // If node has been matched, don't count as spur
                distance = node.getSwcData().getSecondaryData().getPathLength();
                if (distance < threshold && !matches.containsKey(node)) {
                    if (isWithinXYThreshold(xyCheck, node.getSwcData()))
                        System.out.println("Is Spur");
                    bothChildrenAreSpurs = false;
                    // Add to spurs list
                    spurList.add(node);
                    if (node.isRight()) {
                        // Node is right, left hasn't been checked yet
                        newNode = node.getParent().getParentedLeft();
                        // Check to see if sibling is also spur
                        if (!newNode.hasChildren() && newNode.getSwcData().getSecondaryData().getPathLength() < threshold
                                && !matches.containsKey(node)) {
                            if (isWithinXYThreshold(xyCheck, newNode.getSwcData()))
                                System.out.println("Is Spur: " + getPositionString(newNode));
                            bothChildrenAreSpurs = true;
                            // Remove left node from stack (and add to spurs list)
                            spurList.add(newNode);
                            stack.pop();
                        }
                    }

                    if (!bothChildrenAreSpurs) {
                        // Parent is not really a node now (is a continuation)
                        spurList.add(node.getParent());
                    }
                }
            }
        }
        return root;
    }

    private EuclideanPoint getTrajectoryForPath(ParentedBinaryTreeNode ancestorNode, ParentedBinaryTreeNode descendantNode) {
        LinkedStack pathStack = new LinkedStack();
        SwcDataNode ancestorData = ancestorNode.getSwcData();
        // Load nodes along path into a stack, from descendant up to ancestor
        while (descendantNode != ancestorNode) {
            pathStack.push(descendantNode);
            descendantNode = descendantNode.getParent();
            if (descendantNode == null) {
                // This should not happen if descendantNode is a descendant of ancestorNode
                return null;
            }
        }

        // Now we can start with the ancestor's immediate child and work down
        ParentedBinaryTreeNode nextDescendant;
        SwcSecondaryData secData;
        EuclideanPoint trajectory = new EuclideanPoint(), tmp;

        // Assumes original descendantNode != ancestorNode
        descendantNode = (ParentedBinaryTreeNode) pathStack.pop();
        boolean doneX = false, doneZ = false;

        while (!pathStack.isEmpty() && (!doneX || !doneZ)) {
            secData = descendantNode.getSwcData().getSecondaryData();
            // Pull off next node down too to check which child trajectory to use
            nextDescendant = (ParentedBinaryTreeNode) pathStack.pop();

            // First see if descendantNode has a trajectory to nextDescendant
            if (nextDescendant.isLeft()) {
                if (!doneX && secData.getLeftTrajectoryPoint().getX() != ReadSWC.TRAJECTORY_NONE) {
                    trajectory.setX(secData.getLeftTrajectoryPoint().getX());
                    trajectory.setY(secData.getLeftTrajectoryPoint().getY());
                    doneX = true;
                }
                if (!doneZ && secData.getLeftTrajectoryPoint().getZ() != ReadSWC.TRAJECTORY_NONE) {
                    trajectory.setZ(secData.getLeftTrajectoryPoint().getZ());
                    doneZ = true;
                }
            } else if (nextDescendant.isRight() && secData.getRightTrajectoryPoint() != null) {
                if (!doneX && secData.getRightTrajectoryPoint().getX() != ReadSWC.TRAJECTORY_NONE) {
                    trajectory.setX(secData.getRightTrajectoryPoint().getX());
                    trajectory.setY(secData.getRightTrajectoryPoint().getY());
                    doneX = true;
                }
                if (!doneZ && secData.getRightTrajectoryPoint().getZ() != ReadSWC.TRAJECTORY_NONE) {
                    trajectory.setZ(secData.getRightTrajectoryPoint().getZ());
                    doneZ = true;
                }
            }
            // Otherwise descendant and next are close;
            // If threshold distance has been reached from ancestor, calculate point between descendants

            if (!doneX && !isWithinXYThreshold(ancestorData, nextDescendant.getSwcData())) {
                tmp = ReadSWC.calculateTrajectoryXY(ancestorData, descendantNode.getSwcData(), nextDescendant.getSwcData(), XYThreshold);
                trajectory.setX(tmp.getX());
                doneX = true;
            }
            if (!doneZ && (Math.abs(ancestorData.getZ() - nextDescendant.getSwcData().getZ()) > ZThreshold)) {
                trajectory.setZ(ReadSWC.calculateTrajectoryZ(ancestorData, descendantNode.getSwcData(), nextDescendant.getSwcData(), ZThreshold));
                doneZ = true;
            }

            // Move down by setting nextDescendant as current one for trajectory check
            descendantNode = nextDescendant;
        }

        if (!doneX) {
            // Distance from ancestor to descendant is within threshold distance
            // Last descendantNode should be the original from the input parameters
            trajectory.setX(descendantNode.getSwcData().getX());
            trajectory.setY(descendantNode.getSwcData().getY());
        }
        if (!doneZ) {
            trajectory.setZ(descendantNode.getSwcData().getZ());
        }

        return trajectory;
    }

    private double getEndNodeXYDistanceDifference(SwcDataNode goldData, EuclideanPoint trajectory, SwcDataNode testData) {
        double goldDist = SwcDataUtils.getXYDistance(goldData, trajectory);
        double testDist = SwcDataUtils.getXYDistance(testData, trajectory);
        return goldDist - testDist;
    }

    private double getEndNodeZDistanceDifference(SwcDataNode goldData, EuclideanPoint trajectory, SwcDataNode testData) {
        double goldDist = Math.abs(goldData.getZ() - trajectory.getZ());
        double testDist = Math.abs(testData.getZ() - trajectory.getZ());
        return goldDist - testDist;
    }

    private double getDistance(BinaryTreeNode node1, BinaryTreeNode node2) {
        return SwcDataUtils.getDistance(node1.getSwcData(), node2.getSwcData());
    }

    private double getXYDistance(BinaryTreeNode node1, BinaryTreeNode node2) {
        return SwcDataUtils.getXYDistance(node1.getSwcData(), node2.getSwcData());
    }

    private double getZDistance(BinaryTreeNode node1, BinaryTreeNode node2) {
        return Math.abs(node1.getSwcData().getZ() - node2.getSwcData().getZ());
    }

    private boolean isWithinThreshold(BinaryTreeNode node1, BinaryTreeNode node2) {
        return isWithinThreshold(node1.getSwcData(), node2.getSwcData());
    }

    private boolean isWithinThreshold(SwcDataNode node1, SwcDataNode node2) {
        return (SwcDataUtils.getXYDistance(node1, node2) <= XYThreshold
                && Math.abs(node1.getZ() - node2.getZ()) <= (ZThreshold + 0.1));
        // ZThreshold increased fractionally to avoid potential floating point error
    }

    private boolean isWithinXYThreshold(EuclideanPoint point1, EuclideanPoint point2) {
        return (SwcDataUtils.getXYDistance(point1, point2) <= XYThreshold);
    }

    private boolean isWithinDistantMatchThreshold(BinaryTreeNode node1, BinaryTreeNode node2) {
        return isWithinDistantMatchThreshold(node1.getSwcData(), node2.getSwcData());
    }

    private boolean isWithinDistantMatchThreshold(SwcDataNode node1, SwcDataNode node2) {
        return (SwcDataUtils.getXYDistance(node1, node2) <= XYThreshold * 3
                && Math.abs(node1.getZ() - node2.getZ()) <= ZThreshold * 3 + 0.1);
    }

    private String getPositionString(EuclideanPoint node) {
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(3);
        fmt.setGroupingUsed(false);
        if (scaleZ != 0) {
            return "(" + fmt.format(node.getX()) + "," + fmt.format(node.getY()) + "," + fmt.format(node.getZ() / scaleZ) + ")";
        } else {
            return "(" + fmt.format(node.getX()) + "," + fmt.format(node.getY()) + ")";
        }
    }

    private String getPositionString(BinaryTreeNode node) {
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(3);
        fmt.setGroupingUsed(false);
        if (scaleZ != 0) {
            return "(" + fmt.format(node.getSwcData().getX()) + "," + fmt.format(node.getSwcData().getY()) + "," + fmt.format(node.getSwcData().getZ() / scaleZ) + ")";
        } else {
            return "(" + fmt.format(node.getSwcData().getX()) + "," + fmt.format(node.getSwcData().getY()) + ")";
        }
    }

    private static JSAPResult readParameters(String[] args) throws Exception {
        JSAP jsap = new JSAP();

        FlaggedOption opt;

        opt = new FlaggedOption(PARAM_TEST_DATA)
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('T')
                .setLongFlag(PARAM_TEST_DATA);
        jsap.registerParameter(opt);

        opt = new FlaggedOption(PARAM_GOLD_STANDARD_DATA)
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('G')
                .setLongFlag(PARAM_GOLD_STANDARD_DATA);
        jsap.registerParameter(opt);

        opt = new FlaggedOption(PARAM_DATASET)
                .setStringParser(JSAP.INTEGER_PARSER)
                .setRequired(true)
                .setShortFlag('D')
                .setLongFlag(PARAM_DATASET);
        opt.setHelp("            1: Cerebellar Climbing Fibers \n" +
                "            2: Hippocampal CA3 Interneurons \n" +
                "            3: Neocortical Layer 6 Axons \n" +
                "            4: Neuromuscular Projection Fibers \n" +
                "            5: Olfactory Projection Fibers");
        jsap.registerParameter(opt);

        opt = new FlaggedOption(PARAM_MISSES)
                .setStringParser(JSAP.BOOLEAN_PARSER)
                .setRequired(false)
                .setDefault("false")
                .setShortFlag('m')
                .setLongFlag(PARAM_MISSES);
        jsap.registerParameter(opt);

        if (testEnvironment) {
            opt = new FlaggedOption(PARAM_WEIGHTED)
                    .setStringParser(JSAP.BOOLEAN_PARSER)
                    .setRequired(false)
                    .setDefault("false")
                    .setShortFlag('w')
                    .setLongFlag(PARAM_WEIGHTED);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_XY_THRESHOLD)
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("1")
                    .setShortFlag('t')
                    .setLongFlag(PARAM_XY_THRESHOLD);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_Z_THRESHOLD)
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("1")
                    .setShortFlag('z')
                    .setLongFlag(PARAM_Z_THRESHOLD);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_XY_PATH_THRESHOLD)
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("1")
                    .setShortFlag(JSAP.NO_SHORTFLAG)
                    .setLongFlag(PARAM_XY_PATH_THRESHOLD);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_Z_PATH_THRESHOLD)
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("1")
                    .setShortFlag(JSAP.NO_SHORTFLAG)
                    .setLongFlag(PARAM_Z_PATH_THRESHOLD);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_CONTINUATIONS)
                    .setStringParser(JSAP.BOOLEAN_PARSER)
                    .setRequired(false)
                    .setDefault("false")
                    .setShortFlag('c')
                    .setLongFlag(PARAM_CONTINUATIONS);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_DISTANT_MATCHES)
                    .setStringParser(JSAP.BOOLEAN_PARSER)
                    .setRequired(false)
                    .setDefault("false")
                    .setShortFlag('d')
                    .setLongFlag(PARAM_DISTANT_MATCHES);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_Z_PATH)
                    .setStringParser(JSAP.BOOLEAN_PARSER)
                    .setRequired(false)
                    .setDefault("false")
                    .setShortFlag(JSAP.NO_SHORTFLAG)
                    .setLongFlag(PARAM_Z_PATH);
            jsap.registerParameter(opt);

            opt = new FlaggedOption(PARAM_REMOVE_SPURS)
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("0")
                    .setShortFlag('r')
                    .setLongFlag(PARAM_REMOVE_SPURS);
            jsap.registerParameter(opt);

            opt = new FlaggedOption("X")
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("0")
                    .setShortFlag('X')
                    .setLongFlag("X");
            jsap.registerParameter(opt);

            opt = new FlaggedOption("Y")
                    .setStringParser(JSAP.DOUBLE_PARSER)
                    .setRequired(false)
                    .setDefault("0")
                    .setShortFlag('Y')
                    .setLongFlag("Y");
            jsap.registerParameter(opt);

            opt = new FlaggedOption("debug")
                    .setStringParser(JSAP.BOOLEAN_PARSER)
                    .setRequired(false)
                    .setDefault("false")
                    .setShortFlag(JSAP.NO_SHORTFLAG)
                    .setLongFlag("debug");
            jsap.registerParameter(opt);
        }

        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            for (java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }
            System.err.println();
            System.err.println("Usage: java -jar DiademMetric.jar");
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            // show full help as well
            System.err.println(jsap.getHelp());
            System.exit(1);
        }

        return config;
    }

    public static void main(String args[]) {
        try {
            String gtdirname = "F:\\u\\data_newmetric\\diadem_dataset\\diadem_gt";
            String predirname = "F:\\u\\data_newmetric\\diadem_dataset\\diadem_app2";
            File f1 = new File(gtdirname);
            if (f1.isDirectory()) {
                System.out.println("目录 " + gtdirname);
                String s[] = f1.list();
                for (int i = 0; i < s.length; i++) {
                    File f = new File(gtdirname + "/" + s[i]);
                    System.out.println(f);
                    File ff = new File(predirname + "/" + s[i]);
//					System.out.println(ff);
                    String goldSwcFilename = f.toString();
                    String testSwcFilename = ff.toString();

                    int dataset = 1;
                    File testSwcFile = new File(testSwcFilename);
                    File goldSwcFile = new File(goldSwcFilename);

                    // Validate parameters
                    validate(goldSwcFilename, testSwcFilename, dataset);

                    // Create DiademMetric object and assign any relevant parameters
                    DiademMetric metric = new DiademMetric(testSwcFile, goldSwcFile, dataset);
                    metric.setListMisses(false);

                    // 存文件名
                    File result_file = null;
                    FileWriter fw = null;
                    result_file = new File("F:\\u\\result\\metric12\\diadem\\dataset\\dataset_5_result.txt");
                    if (!result_file.exists()) {
                        result_file.createNewFile();
                    }
                    BufferedWriter output = new BufferedWriter(new FileWriter(result_file, true));
                    output.write(testSwcFilename + ":\n");
                    output.flush();
                    output.close();

                    // Run the metric
                    metric.scoreReconstruction();

                }
            }
//
        } catch (DataFormatException e) {
            System.out.println("Failure due to: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//为了适应1741
//    public static void main(String args[]) {
//        try {
//            String predirname = "F:\\u\\1741\\diadem\\sorted_recon";//重建
//            String gtdirname = "F:\\u\\1741\\diadem\\gs_refined_512";//金标准
//            File f1 = new File(predirname);
//            System.out.println(f1);
//            if (f1.isDirectory()) {
//                //            System.out.println("目录 " + predirname);
//                String brain[] = f1.list();//s存储脑编号
//                for (int i = 0; i < brain.length; i++) {
//                    File f2 = new File(predirname + "/" + brain[i]);
//                    //                System.out.println(f2);//f是有毛编号的文件夹地址
//                    if (f2.isDirectory()) {
//                        String s[] = f2.list();
//                        for (int a = 0; a < s.length; a++) {
//                            File f = new File(f2 + "/" + s[a]);
//                            System.out.println(f);//f就是重建的swc
//
//                            File ff = new File(gtdirname + "/" + brain[i] + "/" + s[a]);
//                            System.out.println(ff);
//
//                            if (ff.exists()){
//                                String goldSwcFilename = f.toString();
//                                String testSwcFilename = ff.toString();
//
//                                int dataset = 1;
//                                File testSwcFile = new File(testSwcFilename);
//                                File goldSwcFile = new File(goldSwcFilename);
//
//                                // Validate parameters
//                                validate(goldSwcFilename, testSwcFilename, dataset);
//
//                                // Create DiademMetric object and assign any relevant parameters
//                                DiademMetric metric = new DiademMetric(testSwcFile, goldSwcFile, dataset);
//                                metric.setListMisses(false);
//
//                                // 存文件名
//                                File result_file = null;
//                                FileWriter fw = null;
//                                result_file = new File("F:\\u\\result\\metric12\\diadem\\dataset\\dataset_1_result.txt");
//                                if (!result_file.exists()) {
//                                    result_file.createNewFile();
//                                }
//                                BufferedWriter output = new BufferedWriter(new FileWriter(result_file, true));
//                                output.write(testSwcFilename + ":");
//                                output.flush();
//                                output.close();
//
//                                // Run the metric
//                                try{
//                                    metric.scoreReconstruction();
//                                }catch (NullPointerException npe){
//                                    System.out.println(ff);
//                                }
//
//
//                            }
//
//                        }
//                    }
//                }
//            }
////
//        } catch (
//                DataFormatException e) {
//            System.out.println("Failure due to: " + e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


    private static void validate(String goldSwcFilename, String testSwcFilename, int dataset) {
        File testSwcFile = new File(testSwcFilename);
        File goldSwcFile = new File(goldSwcFilename);

        boolean stop = false;
        if (!testSwcFile.exists()) {
            System.out.println("Test file " + testSwcFilename + " does not exist");
            stop = true;
        } else if (!testSwcFile.isDirectory() && !testSwcFilename.endsWith(".swc") && !testSwcFilename.endsWith(".SWC")) {
            System.out.println("Test file " + testSwcFilename + " is not an SWC file or directory");
            stop = true;
        }
        if (!goldSwcFile.exists()) {
            System.out.println("Gold standard file/directory " + goldSwcFilename + " does not exist");
            stop = true;
        } else if (!goldSwcFile.isDirectory() && !goldSwcFilename.endsWith(".swc") && !goldSwcFilename.endsWith(".SWC")) {
            System.out.println("Gold standard file/directory " + goldSwcFilename + " is not an SWC file or directory");
            stop = true;
        }
        if (!stop &&
                ((goldSwcFile.isDirectory() && !testSwcFile.isDirectory()) ||
                        (!goldSwcFile.isDirectory() && testSwcFile.isDirectory()))) {
            System.out.println("Gold standard and test parameters must both be swc files or both be directories");
            stop = true;
        }
        if (dataset < 1 || dataset > 5) {
            System.out.println("Data set number invalid, please use one of the following: \n" +
                    "            1: Cerebellar Climbing Fibers \n" +
                    "            2: Hippocampal CA3 Interneurons \n" +
                    "            3: Neocortical Layer 6 Axons \n" +
                    "            4: Neuromuscular Projection Fibers \n" +
                    "            5: Olfactory Projection Fibers");
            stop = true;
        }

        if (stop) {
            System.exit(0);
        }

    }

}
