
package de.schildbach.wallet.ui.message;


import java.math.BigInteger;

class   Message {


    // HOW IT WORKS:
    // DECODED MESSAGE: Always a string
    // ENCODED MESSAGE: Always an int[] array

    // To send a message, we take a string, encode it in the form of
    // int[]{integer, code, for, each, char, of, the, string}
    // From this we make the word integercodeforeachcharofthestring and split it
    // into amount 10.integerc 10.odeforea 10.chcharof 10.thestrin 10.g0000000

    private String message;

    public Message(String message) {
        this.message = message;
    }

    // The encoding defined for this testrun.
    // Huffman coding based on the frequency of these characters in English.
    private char[] codeSet = new char[]{'e', 't', 'a', 'o', 'i', 'n', 's', 'h', 'r', 'd', 'l', 'c', 'u', 'm', 'w', 'f', 'g', 'y', 'p', 'b', 'v', 'k', 'j', 'x', 'q', 'z'};
    private int[] codeKeys = new int[]{  1,   2,   3,   4,   5,   6,   7,   80,  81,  82,  83,  84,  85,  86,  87,  88,  90,  91,  92,  93,  94,  95,  96,  97,  98,  99};

    public BigInteger[] amountsFromMessage() {
        return amountsFromIntSeq(this.encode());
    }

    public String messageFromAmounts(BigInteger[] amounts) {
        //return decode(intSeqFromAmounts());
        return null;
    }

    public BigInteger[] decodeAndGetValue() {
        return null;//return message.decode(message.)
    }


    // Use: encodedString = encode(str)
    // Pre: str is a string using only ASCII characters a, b, c, ... , z
    // Post: encodedString is str encoded as a decimal number using the coding defined above.
    private int[] encode() {
        // We will put the numbers in an array so it can easily be decoded
        int[] encoding = new int[message.length()];
        char[] characters = message.toCharArray();

        for(int j = 0; j < characters.length; j++) {
            int i = indexOf(codeSet, characters[j]);
            if(i < 0) {
                System.out.println("Error, invalid input");
                System.exit(0);
            }
            int cEncoded = codeKeys[i];
            encoding[j] = cEncoded;
        }

        return encoding;
    }

    // Use: decodedString = decode(seq)
    // Pre: seq is a decimal number
    // Post: decodedString is a string decoded from seq using the coding defined above.
    private String decode(int[] intSequence) {
        String decodedStr = "";

        for(int key : intSequence) {
            //int i = codeKeys.indexOf(key);
            //int i = java.util.Arrays.binarySearch(codeKeys, key);
            int i = indexOf(codeKeys, key);
            if(i < 0) {
                System.out.println("Error, invalid input");
                System.exit(0);
            }
            char cDecoded = codeSet[i];
            decodedStr += cDecoded;
        }

        return decodedStr;
    }

    // Takes the integer sequence from encoding the message and transforms
    // it into amounts
    private BigInteger[] amountsFromIntSeq(int[] intseq) {
        // Append integers to string to count the digits
        String s = "";
        for(int i : intseq) s += Integer.toString(i);

        int n = s.length();
        // Append zero's to s so we have a string that's multiples of 8
        if(n%8 != 0) {
            for(int i = 0; i<(8 - n%8); i++) s += "0";
        }

        // Int sequences of 8, in BigInt
        int nSeqs = s.length() / 8;
        BigInteger[] stripsOfEight = new BigInteger[nSeqs];
        int k = 0; // To count the length of digits gathered
        int j = 0; // Index to the array.
        while(k < s.length()) {
            stripsOfEight[j] = new BigInteger(s.substring(k, k+8));
            System.out.println("Strips of eight no "+j+" is: "+stripsOfEight[j]);
            j++;
            k += 8;
        }
        /*
        // Create an array to hold every 8 digits that occur
        int nSeqs = s.length() / 8;
        int[] stripsOfEight = new int[nSeqs];
        int k = 0; // To count the length of digits gathered
        int j = 0; // Index to the array.
        while(k < s.length()) {
            stripsOfEight[j] = Integer.parseInt(s.substring(k, k+8));
            System.out.println("Strips of eight no "+j+" is: "+stripsOfEight[j]);
            j++;
            k += 8;
        }
        */

        BigInteger[] amounts = new BigInteger[nSeqs+1];
        amounts[0] = new BigInteger("101000000000"); //  + nSeqs; This way, every message starts with 1010.numberOfTransactionsThatFollow
        BigInteger basePrice = new BigInteger("1000000000");
        for(int i = 0; i < nSeqs; i++) {
            amounts[i+1] = basePrice.add(stripsOfEight[i]);
            System.out.println("Amount no "+(i+1)+" is: "+amounts[i+1]);
        }

        return amounts;
    }

    // Takes the array of amounts and transforms it back into
    // an integer sequence from which we can decode the message
    private int[] intSeqFromAmounts(double[] amounts) {
        // Take the extra digits and append them to a string:
        String encodedWord = "";
        for(int i = 1; i<amounts.length; i++) {
            double d = amounts[i];
            int temp = (int)((d-10.0)*100000000);
            System.out.println("Printing ints... "+d+" " + temp);
            encodedWord += Integer.toString((int)((d-10.0)*100000000));
        }

        System.out.println("These are the amounts "+toString(amounts));
        System.out.println("Just see if this happens");
        System.out.println("This is the encoded word: "+encodedWord);

        int index = 0; // The index in the string 'encodedWord'
        int keyLength = 1; // How many digits we try to parse as a character. From CodeKeys above.
        boolean keepLooking=true; // If we can't find the single digit, we'll keep looking
        String sequence = "";

        while(index < encodedWord.length()) {
            keyLength = 1;
            keepLooking = true;
            while(keepLooking) {
                int key = Integer.parseInt(encodedWord.substring(index, keyLength));
                System.out.println("Looking for this key "+key);
                int i = indexOf(codeKeys, key);
                System.out.println("Did we find it in codekeys? This is the index "+i);
                if(i > -1) {
                    keepLooking = false;
                    sequence = codeKeys[i] + " ";
                }
                System.out.println("Sequence is now: "+sequence);
            }

            index += keyLength;
        }

        String[] strSeq = sequence.split(" ");
        int[] intSeq = new int[strSeq.length];
        for(int i = 0; i < strSeq.length; i++) {
            intSeq[i] = Integer.parseInt(strSeq[i]);
        }

        return intSeq;
    }


    // ====
    // MAIN
    // ====

    public static void main(String[]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             args) {

        Message msg = new Message("this is a dummy message");

        if(args.length == 0) {
            System.out.println("You need to run the program with an argument. Exiting...");
            System.exit(0);
        }

        String word = args[0];
        int[] encoding = msg.encode();
        /*double[] amounts = msg.amountsFromIntSeq(encoding);
        int[] intSeq = msg.intSeqFromAmounts(amounts);
        //String decoding = msg.decode(intSeq);


        System.out.println("You entered: "+args[0]);
        System.out.println("That was encoded to "+msg.toString(encoding));
        System.out.println("The amounts we got: "+msg.toString(amounts));
        System.out.println("From the amounts, we get: "+msg.toString(intSeq));
        //System.out.println("That can be decoded to "+decoding);*/
    }


    // ==============
    // UTIL FUNCTIONS
    // ==============

    private int indexOf(char[] cArray, char c) {
        for(int i=0; i<cArray.length; i++) {
            if(cArray[i] == c) return i;
        }

        return -1;
    }

    private int indexOf(int[] iArray, int c) {
        for(int i=0; i<iArray.length; i++) {
            if(iArray[i] == c) return i;
        }

        return -1;
    }

    private String toString(int[] iArray) {
        String s = "";
        for(int i : iArray) s = s + " " + Integer.toString(i);

        return s;
    }


    private String toString(double[] dArray) {
        String s = "";
        for(double d : dArray) s = s + " " + Double.toString(d);

        return s;
    }
}