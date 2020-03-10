import java.util.PriorityQueue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;

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
    }

	private static final char EMPTY_CHARACTER = 0;
	private static HashMap<Character, String> hashmap = new HashMap<Character, String>();
	
    public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException 
    { 
// Code page atkļūdošanai
//    	System.setProperty("file.encoding","UTF-8");
//    	Field charset = Charset.class.getDeclaredField("defaultCharset");
//    	charset.setAccessible(true);
//    	charset.set(null,null);
    	Huffman mHuf = new Huffman();
    	HuffmanNode root = null;
    	PriorityQueue<HuffmanNode> que = null;
    	PriorityQueue<HuffmanNode> que2 = null;
    	try {
    		que = mHuf.nodeListGen("char_count.txt",false);
    		que2 = new PriorityQueue<HuffmanNode>(que);
			root = mHuf.treeGen(que);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	System.out.println(root.data + "a");
    	printCode(root, "");
  
    	try {
			mHuf.fileEncoder(hashmap, "char_count.txt", que2);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    // TODO : izņemt šo metodi, bet pēc viņas principa izveidot 
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
    public void binFill(PriorityQueue<HuffmanNode> tree, Reader fr) throws Exception
    {
    	int i, iBinTotal, nodeF = 0, pos = 0;
    	String sBinTotal = "";
    	
    	// Pārlasām pāri vārdnīcas ievaddatiem
    	// Nolasām vārdnīcas kopējo baitu izmēru
		while ((i = fr.read()) != -1 && (char) i != ' ')
		{
			sBinTotal += (char) i;
			pos++;
		} 
		iBinTotal = Integer.parseInt(sBinTotal);
		
		while (pos < iBinTotal)
		{
			sBinTotal = "";
			i = fr.read(); // nolasām simbolu
			pos++; // palielinām apstrādāto baitu
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
				throw new Exception("Kļūda nolasot failu : Atkārtojas simboli vārdnīca!");
			}
			else
			{
				tree.add(new HuffmanNode(nodeF,nodeC));
			}
		}
    }
    // Aizpilda koka sarakstu no plain-text faila
    public void plainFill(PriorityQueue<HuffmanNode> tree, Reader fr) throws IOException
    {	
    	int i;
		while ((i = fr.read()) != -1)
		{
			final char ch = (char) i;
			// Ja kādai virsotnei piemīt šis simbols, tad mēs palielinām biežumu par 1, citādi
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
 
    public void fileEncoder(HashMap<Character, String> enc_map, String filePath, PriorityQueue<HuffmanNode> que) throws IOException {
    	// Jaunā faila nosaukums
    	String out_filename = "C:\\Users\\marti\\git\\DreamTeamCompress\\src\\encoded_file.txt";
    	// Savāc bibliotēku iekš sevis
    	String frequencies = " ";
    	while(que.size() > 0) {
    		HuffmanNode node = que.peek();
    		que.poll();
    		frequencies += node.character + " " + node.data + " ";
    	}
    	byte[] bytes = null;
		try {
			bytes = frequencies.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Iegūst baitus pirms satura (vajadzības gadījumā var pieskaitīt skaitli, lai būtu buffer)
    	int bytes_to_content = bytes.length;//šeit ir jādod tik baiti, cik skaitlim cipari nevis vnk 2
    	int old_content = bytes_to_content;
    	do {
    	old_content = bytes_to_content;	
    	bytes_to_content += Integer.toString(bytes_to_content).length();
    	}while(Integer.toString(old_content).length() != Integer.toString(bytes_to_content).length());
    	
    	// Izveido jaunu failu
    	File output_file = new File(out_filename);
    	// Atver failu lasīšanai
    	InputStream is = null;
		try {
			is = new FileInputStream(filePath);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	Reader fr = new InputStreamReader(is, Charset.forName("UTF-8"));
    	int r;
    	// Faila rakstītājs
    	String bin_string = "";
    	String char_string = "";
    	char character;
    	try {
    		FileWriter fwr = new FileWriter(out_filename);		
        	fwr.write(bytes_to_content + frequencies);//lieks space, kas arī pieliek +1 baitu izmēros
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
        	fwr.close();
        	// aizveram simbolu rakstīšanu un atveram bināru rakstīšanu 
        	// ar karogu true priekš append, lai rakstītu klāt, nevis pārrakstītu
        	FileOutputStream fos = new FileOutputStream(out_filename, true);
        	// Pēc tam iterē pāri mainīgajam, skalda daļās, katru daļu uz baitu
        	// sākotneji bināro stringu uztaisam uz integer, pēc tam nokāstojam uz baitu
        	// jo baitu naturāli var izveidot tikai vērtībām līdz 127(0b01111111)
        	
        	for (int i = 0; i < bin_string.length(); i += 8) {
        		fos.write((byte)Integer.parseInt(bin_string.substring(i, i+8), 2));
        	}
        	
    	} catch (IOException e) {
    		System.out.println("Notikusi kļūda");
    	}
    	fr.close(); // aizstaisam lasīšanu, iepriekš aizmirsi pielikt.
    	System.out.println("Saspiešana pabeigta");
    }
    
    
    
    /**  
     * <h1>Metode, kas nolasa failu, iegūst simbolu biežumus un izveido koku.</h1>
     * <br> 
     * @param fPath : Saņem ceļu uz failu
     * @param isBinary : Nosaka vai koku veido no saspiesta vai plain-text faila
     * @return HuffmanNode - saknes virsotne
     * @throws IOException Ja nevar nolasīt no faila simbolu
     * */
public PriorityQueue<HuffmanNode> nodeListGen(String fPath, Boolean isBinary) throws IOException{
	Izveido prioritātes rindu, kur elementi 
	// tiek kartoti pēc compare metodes(simbola biežuma)
	// Padod sākotnējos izmērus(pēc noklusējuma ir 11) un metodi, kas salīdzina elementus
	PriorityQueue<HuffmanNode> tree = new PriorityQueue<HuffmanNode>(11, 
		new  Comparator<HuffmanNode>()
		{
			public int compare(HuffmanNode arg0, HuffmanNode arg1) {
				return arg0.data - arg1.data;
			}
		}
	);
	
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
	return tree;
	}
    public HuffmanNode treeGen(PriorityQueue<HuffmanNode> tree) throws IOException 
    {  	
    	// Pašlaik ir sakārtota rinda ar atsevišķām virsotnēm
    	// Nepieciešams virsotnes apvienot kokā!
    	
    	// Izveidojam saknes virsotni, no kuras sāks lasīt šifrētās vērtības
    	HuffmanNode root = null;
    	
    	// Izveidojam koka struktūru
    	while (tree.size()>1)
    	{
    		HuffmanNode one = tree.peek();
    		tree.poll();
    		
    		HuffmanNode two = tree.peek();
    		tree.poll();
    		
    		HuffmanNode combo = new HuffmanNode(0, EMPTY_CHARACTER);
    		combo.data = one.data + two.data;
    		combo.left = one;
    		combo.right = two;
    		
    		root = combo;
    		tree.add(combo);
    	}
    	
    	// atgriežam saknes virsotni.
		return root;
    }
}