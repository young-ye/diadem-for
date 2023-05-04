import java.io.File;

public class test {
    public static void main(String[] args) {

        String predirname = "F:\\u\\data_newmetric\\diadem\\new_59\\mix_resample";//重建
        String gtdirname = "F:\\u\\data_newmetric\\diadem\\new_59\\gt_resample";//金标准
        File f1 = new File(predirname);
        System.out.println(f1);
        if (f1.isDirectory()) {
//            System.out.println("目录 " + predirname);
            String brain[] = f1.list();//s存储脑编号
            for (int i = 0; i < brain.length; i++) {
                File f2 = new File(predirname + "/" + brain[i]);
//                System.out.println(f2);//f是有毛编号的文件夹地址
                if (f2.isDirectory()) {
                    String s[] = f2.list();
                    for (int a = 0; a < s.length; a++) {
                        File f = new File(f2 + "/" + s[a]);
                        System.out.println(f);//f就是重建的swc

                        File ff = new File(gtdirname + "/" + brain[i] + "/" + s[a]);
					    System.out.println(ff);

                        if (ff.exists()){
                            String goldSwcFilename = f.toString();
                            String testSwcFilename = ff.toString();

                        }

                    }
                }
            }



        }
    }
}
//have a test