
    public static String reshape(int n, String str) {

        String cleanStr="";
        for(char c : str.toCharArray()) {
            if(c!=' ') {
                cleanStr+=c;
            }
        }

        String result = "";
        for(int i=0; i<cleanStr.length(); ++i) {
            if(i>0 && i%n==0) {
                result+='\n';
            }
            result+=cleanStr.toCharArray()[i];
        }

        return result;
    }

    public static void main(String[] args) {
       System.out.println(reshape(3, "abc de fghi j"));
    }