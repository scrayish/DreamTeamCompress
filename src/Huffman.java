import java.util.PriorityQueue;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Huffman {

    class HuffmanNode {
int data;
        char character;
        HuffmanNode left;
        HuffmanNode right;

        public HuffmanNode(int i, char c) {
        this.data = i;
        this.character = c;
        left = null;
        right = null;
}

public HuffmanNode(HuffmanNode root) {
        this.data = root.data;
        this.character = root.character;
        left = root.left;
        right = root.right;
}
    }

    class ListComparator implements Comparator<HuffmanNode> {
        public int compare(HuffmanNode arg0, HuffmanNode arg1)
        {

    if (arg0.data<arg1.data)
return -1;
if (arg0.data>arg1.data)
return 1;
if (arg0.data == arg1.data){

if (arg0.character > arg1.character)
return 1;
if (arg0.character < arg1.character)
return -1;
}
return 0;
    }
    }

private static final char EMPTY_CHARACTER = 0;
private static HashMap<Character, String> hashmap = new HashMap<Character, String>();

    public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException,
    			IllegalAccessException, IOException {
    		// Enter data using BufferReader
    		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    		String filename = null;
    		System.out.println("Vai izvēlēties failu 'char_count.txt'? (y/n)");
    		char ans = (char) System.in.read();
    		if (ans == 'y' || ans == 'Y') {
    			filename = "char_count.txt";
    			System.in.read(new byte[System.in.available()]);
    		} else {
    			System.in.read(new byte[System.in.available()]);
    			System.out.println("Ievadiet faila nosaukumu:");
    			filename = reader.readLine();
    			if (filename.length() == 0) {
    				filename = "char_count.txt";
    			}
    		}
    		System.out.println("Faila izmantojamā faila nosaukums: " + filename);
    		System.in.read(new byte[System.in.available()]);
    		System.out.println("Ievadiet beigu faila nosaukumu:");
    		String result_filename = reader.readLine();
    		if (result_filename.length() == 0) {
    			result_filename = "char_count_out.txt";
    		}
    		System.out.println("Beigu faila nosaukums: " + result_filename);

    		// Code page atkļūdošanai
    		// System.setProperty("file.encoding","UTF-8");
    		// Field charset = Charset.class.getDeclaredField("defaultCharset");
    		// charset.setAccessible(true);
    		// charset.set(null,null);
    		Huffman mHuf = new Huffman();
    		HuffmanNode root = null;
        	List<HuffmanNode> nodeList = new ArrayList<HuffmanNode>();
        	List<HuffmanNode> nodeList2 = null;

    		boolean end = false;
    		while (true) {

    			System.out.println("Izvēlēties operāciju: (0 - pabeigt darbību; 1 - saspiest; 2 - dekodēt)");
    			ans = (char) System.in.read();
    			System.in.read(new byte[System.in.available()]);
    			switch (ans) {
    			case '1':
    				// encode
    				try {
        		nodeList = mHuf.nodeListGen(filename,false);
        		nodeList2 = new ArrayList<HuffmanNode>(nodeList);
                root = mHuf.treeGen(nodeList);
    			printCode(root, "");
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    				try {
    					mHuf.fileEncoder(hashmap, filename, nodeList2, result_filename);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    				end = true;
    				break;
    			case '2':
    				// decode
    				try {
    						nodeList = mHuf.nodeListGen(filename,true);
        		nodeList2 = new ArrayList<HuffmanNode>(nodeList);
    			root = mHuf.treeGen(nodeList);
                printCode(root, "");
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    try {
    			mHuf.fileDecoder(filename, root, result_filename);

    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    				end = true;
    				break;
    			case '0':
    				end = true;
    				break;
    			default:
    				System.out.println("Ievadītā operācija neeksistē, mēģiniet vēlreiz");
    				break;
    			}
    			if (end) {
    				System.out.println("Darbība beigta");
    				break;
    			}
    		}
    		}

    // TODO : izņemt metodi, bet pēc viņas principa izveidot
    // HashMap, lai vienkāršāk Encode failu
    public static void printCode(HuffmanNode root, String s)
    {

        // base case; if the left and right are null
        // then its a leaf node and we print
        // the code s generated by traversing the tree.
        if (root.left
                == null
            && root.right
                   == null
            && ((int)root.character != 0)) {

            // c is the character in the node
            System.out.println(root.character + ":" + s);
            //Write to hashmap
            hashmap.put(root.character,s);
            return;
        }

        // if we go to left then add "0" to the code.
        // if we go to the right add"1" to the code.

        // recursive calls for left and
        // right sub-tree of the generated tree.
        printCode(root.left, s + "0");
        printCode(root.right, s + "1");
    }
    // Aizpilda koka sarakstu no saspiesta faila
    public void binFill(List<HuffmanNode> tree, Reader fr) throws Exception
    {
    int i, iBinTotal, nodeF = 0, pos = 0;
    String sBinTotal = "";

    // Pārlasām pāri vārdnīcas ievaddatiem
    // Nolasām kopējo baitu izmēru
while ((i = fr.read()) != -1 && (char) i != ' ')
{
sBinTotal += (char) i;
pos++;
}
iBinTotal = Integer.parseInt(sBinTotal);
while ((i = fr.read()) != -1) { // par iet pari bitu skaitam
pos++;
if ((char) i == ' ')
break;
}

while (pos < iBinTotal)
{
sBinTotal = "";
i = fr.read(); // nolas simbolu
pos++; // palielinām apstrādāto  baitu
// Ja varējām nolasīt simbolu, tad pieglabājam
final char nodeC = (i != -1) ? (char) i : 0;

if (nodeC == 0) throw new Exception("Kļūda nolasot failu : Simbols netika atrasts!");

fr.read(); //nolasām atstarpi pirms biežuma
while ((i = fr.read()) != -1 && (char) i != ' ')
{
sBinTotal += (char) i;
pos++;
}
nodeF = Integer.parseInt(sBinTotal);

if (tree.stream().filter(node -> node.character == nodeC).count() > 0)
{
throw new Exception("Kļūda nolasot failu : Atkārtojas simboli vārdnīcā!");
}
else
{
tree.add(new HuffmanNode(nodeF,nodeC));
}
}
    }
    // Aizpilda koka sarakstu no plain-text faila
    public void plainFill(List<HuffmanNode> tree, Reader fr) throws IOException
    {
    int i;

while ((i = fr.read()) != -1)
{
final char ch = (char) i;
// Ja kādai virsotnei pieder tekošais simbols, tad mēs palielinām biežumu par 1, citādi
// pievienojam to rindai.
if (tree.stream().filter(node -> node.character == ch).count() > 0 )
{
tree.parallelStream()
.filter(node -> node.character == ch)
.forEach(node -> node.data++);
}
else
{
tree.add(new HuffmanNode(1, ch));
}
}


    }

    public void fileEncoder(HashMap<Character, String> enc_map, String filePath, List<HuffmanNode> que, String fOut) throws IOException {
    // Jaunā faila nosaukums
    String out_filename = fOut;
    // Savāc bibliotēku iekš sevis
    String frequencies = " ";
    for (HuffmanNode node : que) {
    frequencies += node.character + " " + node.data + " ";
    }

    byte[] bytes = null;
try {
bytes = frequencies.getBytes("UTF-8");
} catch (UnsupportedEncodingException e1) {
e1.printStackTrace();
}

    // Atver failu lasīšanai
    InputStream is = null;
try {
is = new FileInputStream(filePath);
} catch (FileNotFoundException e1) {
e1.printStackTrace();
}
    Reader fr = new InputStreamReader(is, Charset.forName("UTF-8"));
    int r;
    // Faila rakstītājs
    String bin_string = "";
    /* Lasa input failu pa vienai rakstzīmei.
    Kad tā sakrīt ar atslēgu no HashMap, tiek ierakstīts kods mainīgajā. */
    while ((r = fr.read()) != -1) {
    // mūsu hashmap principā ir masīvs, kur indeksi ir simboli
    // līdz ar to nav nepieciešams veidot foreach ciklu.
    /*char c = (char) r;
    for (Character i : enc_map.keySet()) {//get no hashmap dabu pec asociativa array principa
    if (c == i) // lieks cikls un parbaude
    bin_string += enc_map.get(i);
    }*/

    bin_string += enc_map.get((char)r);
    }
    // Iegūst baitus pirms satura (vajadzības gadījumā var pieskaitīt skaitli, lai būtu buffer)
    int bytes_to_content = bytes.length + 2;//Šeit jādod baiti, cik skaitlim cipari nevis vnk 2, bet pec
    // labojuma ir jadod +2 jo atstarpe un 1 cipars ka liekie biti - martins
    int old_content = bytes_to_content;
    do {
    old_content = bytes_to_content;
    bytes_to_content += Integer.toString(bytes_to_content).length();
    }while(Integer.toString(old_content).length() != Integer.toString(bytes_to_content).length());
    fr.close(); // aizstaisam lasīšanu, iepriekš aizmirsi pielikt.
    try {
    FileWriter fwr = new FileWriter(out_filename);
        fwr.write(bytes_to_content + " " + (8-bin_string.length()%8) + frequencies);//lieks space, kas arī pieliek +1 baitu izmēros
        fwr.close();
        // aizveram simbolu rakstīšanu un atveram bināru rakstīšanu
        // ar karogu true priekš append, lai rakstītu klāt, nevis pārrakstītu
        FileOutputStream fos = new FileOutputStream(out_filename, true);
        // Pēc tam iterē pāri mainīgajam, skalda daļās, katru daļu uz baitu
        // sākotneji bināro stringu uztaisam uz integer, pēc tam nokāstojam uz baitu
        // jo baitu naturāli var izveidot tikai vērtībām līdz 127(0b01111111)

        String emptyBits = "";
        for (int i = 0; i< (8-bin_string.length()%8);i++) {
        emptyBits += "0";
        }
        bin_string = emptyBits + bin_string;

        for (int i = 0; i < bin_string.length(); i += 8) {
        String test_byte;
        test_byte = bin_string.substring(i, i+8);
//        	if (i+8<bin_string.length()) {
//        	test_byte = bin_string.substring(i, i+8);
//        	}
//        	else {
//        	test_byte = bin_string.substring(i, bin_string.length());
//        	while (test_byte.length()<8) {
//        	test_byte += "0";
//        	}
//        	}

        fos.write((byte)Integer.parseInt(test_byte, 2));
        }
        fos.close();

    } catch (IOException e) {
    System.out.println("Notikusi kļūda");
    }


    System.out.println("Saspiešana pabeigta");
    }

    public void fileDecoder(String filePath, HuffmanNode root, String fOut) throws IOException
    {
    HuffmanNode curRoot = new HuffmanNode(root);
    String character="", biti ="";
    String newFileName=fOut;
    int c;
    FileReader fr=new FileReader(filePath);
    BufferedReader br=new BufferedReader(fr);
    while((c = br.read()) != -1 ) {  // nolasa baitus

    if ((char)c!=' ')
    character += (char) c;
    else break;
    }
    while((c = br.read()) != -1 ) {   //nolasa bitus

    if ((char)c!=' ')
    biti+= (char) c;
    else break;
    }
    fr.close();
    FileInputStream fin = null;
    FileWriter wr=null;
    byte byteStream[] = null;
    byte curentByteStream;
    char symbol;
    try {
     fin = new FileInputStream(filePath);
     wr= new FileWriter(newFileName);
     fin.skip((long)Integer.parseInt(character));
     int mask = -128;
     mask = (byte)((mask&0xff)>>>Integer.parseInt(biti));
     int j = Integer.parseInt(biti);
             while(fin.available()>0)
             {
             curentByteStream = (byte) fin.read();
            //curentByteStream=byteStream[0];
              for (; j <8 ; j++)
              {

              boolean value = (curentByteStream & (byte)mask ) != 0;

                 if (curRoot.left== null && curRoot.right== null && ((int)curRoot.character != 0))
                 {
               	 wr.write(curRoot.character);
               	 curRoot=root;
               	 //j--;
               	 //mask = (byte)(mask<<1);
                 }
                 else
                 {
               	 if(value)
               	 {
               	 curRoot=curRoot.right;
               	 mask = (byte)((mask&0xff)>>>1);
               	 }
               	 else
               	 {
               	 curRoot=curRoot.left;
               	 mask = (byte)((mask&0xff)>>>1);

               	 }
                 }
     	   	if (curRoot.left== null && curRoot.right== null && ((int)curRoot.character != 0))
       	j--;

              }
              j = 0;
     	   	 mask = -128;

             }
             wr.close();
             fin.close();
         }
         catch (FileNotFoundException e) {
             System.out.println("File not found" + e);
         }
         catch (IOException ioe) {
             System.out.println("Exception while reading file " + ioe);
         }
    }

    /**
     * <h1>Metode, kas nolasa failu, iegūst simbolu biežumus un izveido koku.</h1>
     * <br>
     * @param fPath : Saņem ceļu uz failu
     * @param isBinary : Nosaka vai koku veido no saspiesta vai plain-text faila
     * @return HuffmanNode - saknes virsotne
     * @throws IOException Ja nevar nolasīt no faila simbolu
     * */
public List<HuffmanNode> nodeListGen(String fPath, Boolean isBinary) throws IOException{
//Izveido prioritātes rindu, kur elementi
// tiek kartoti pēc compare metodes(simbola biežuma)
// Padod sākotnējos izmērus(pēc noklusējuma ir 11) un metodi, kas salīdzina elementus
List<HuffmanNode> tree = new ArrayList<HuffmanNode>();

// Atveram failu pirmajai lasīšanai
InputStream is = new FileInputStream(fPath);
Reader fr = new InputStreamReader(is, Charset.forName("UTF-8"));
try
{
if (isBinary == true) binFill(tree, fr); else plainFill(tree, fr);
} catch(Exception e)
{
e.getStackTrace();
}

// Aizveram faila lasīšanu
fr.close();

tree.sort(new ListComparator());

return tree;
}

    public HuffmanNode treeGen(List<HuffmanNode> tree) throws IOException
    {
    // Pašlaik ir sakārtota rinda ar atsevišķām virsotnēm
    // Nepieciešams virsotnes apvienot kokā!

    // Izveidojam saknes virsotni, no kuras sāks lasīt šifrētās vērtības
    HuffmanNode root = null;

    // Izveidojam koka struktūru
    while (tree.size()>1)
    {


    HuffmanNode one = tree.get(0);
    tree.remove(0);

    HuffmanNode two = tree.get(0);
    tree.remove(0);

    HuffmanNode combo = new HuffmanNode(0, EMPTY_CHARACTER);
    combo.data = one.data + two.data;
    combo.left = one;
    combo.right = two;

    root = combo;
    tree.add(combo);

    tree.sort(new ListComparator());
    }

    // atgriežam saknes virsotni.
return root;
    }
}
