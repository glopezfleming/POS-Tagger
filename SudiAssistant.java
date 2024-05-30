
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.io.IOException;
/**
 * @author Gustavo Lopez-Fleming
 *
 * Implements Viterbi Algorithm and Hidden Markov Models to create a part of speech (POS) tagger that labels each word in a sentence with its part of speech (noun, verb, etc.).
 *
 * PS5, Dartmouth CS10, Fall 2023
 */
public class SudiAssistant {

    Map<String, Map<String, Double>> transitionsMap; //holds count for transitions between tags
    Map<String, Map<String, Double>> observationsMap; //holds counts for word-tag observations

    //UNDO UNTIL HERE
    Map<String, Map<String, Double>> trainedTransitionsMap; //holds probability of transitions from tag to tag
    Map<String, Map<String, Double>> trainedObservationsMap; //holds probability of word observation in certain tag

    ArrayList<String> pathSeries; //highest probable path
    ArrayList<String> tagList; //list of tags from input or file


    public ArrayList<String> readFiletags(String tagPath){
        tagList = new ArrayList <String>();
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(tagPath));
            String line;
            while((line = in.readLine()) != null){ //loads tags -> for each line
                String tagsL = line.toLowerCase(); //lowercasify line
                String[] tags = tagsL.split(" "); //store sentence as an array of words
                tagList.add("#"); //add # for the beginning of each sentence
                for(int i =0; i<tags.length; i++){ //for all the tags in the sentence
                    tagList.add(tags[i]); //add all the tags in the file, sentence by sentence, to the list of tags
                }
            }

        }
        catch(IOException e){
            System.out.println(e);
        }
        try{
            in.close();
        }
        catch(IOException e){
            System.out.println(e);
        }
        return tagList;
    }
    public ArrayList<String> readFileSentences(String sentencePath){
        ArrayList <String> wordList = new ArrayList<String>();
        BufferedReader in = null;

        try{
            in = new BufferedReader(new FileReader(sentencePath));
            String line;
            while((line = in.readLine()) != null){ //loads sentences
                String wordsL = line.toLowerCase();
                String[] words = wordsL.split(" "); //store sentence as an array of words
                wordList.add("#");  //add # for the beginning of each sentence
                for(int i=0; i<words.length; i++){ //for all the words in the sentence
                    wordList.add(words[i]); //add all the words in the file, line by line, to the list of words
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        try{
            in.close();
        }
        catch(IOException e){
            System.out.println(e);
        }
        return wordList;
    }

    public void trainMaps(ArrayList<String> tagList, ArrayList<String> wordList){
        transitionsMap = new HashMap<String, Map<String, Double>>(); //Instantiate table of transition frequencies from tag to tag
        observationsMap = new HashMap<String, Map<String, Double>>(); //Instantiate table of word frequencies for each tag

        int i = 0;
        for(String tag: tagList){ //for all the tags in the file
            if (i != tagList.size() -2){
                i = i+1;
                if(transitionsMap.containsKey(tag)){ //checks to see if we've seen the tag already
                    if(transitionsMap.get(tag).containsKey(tagList.get(i))){ //checks to see if we've seen this transition before
                        double addTo = transitionsMap.get(tag).get(tagList.get(i)) +1; //increment transition count by 1
                        transitionsMap.get(tag).put(tagList.get(i), addTo); //accesses column, the puts at the desired column for that row, the updated incremented count
                    }
                    else{
                        transitionsMap.get(tag).put(tagList.get(i), (double)1);//if we don't have this column in our row, start one and start the count
                    }
                }
                else{ //if we are seeing a new tag
                    Map<String, Double> tagCount = new HashMap <String, Double> (); //create a new map (column) for new tag that holds tags and counts
                    tagCount.put(tagList.get(i), (double)1); //.get(i) here avoids the #, builds the columns for each new word we see and starts the count
                    transitionsMap.put(tag, tagCount); //"attatches" those columns to our newly created "column"
                }
            }
        }

        int k =0; //keep track of words in list
        for(String word: wordList){ //for all the words in the file
            //String currTag = uniqueTags.get(t); //table rows will be built from this list
            if(observationsMap.containsKey(tagList.get(k))){ //checks if we have seen tag before
                if(observationsMap.get(tagList.get(k)).containsKey(word)){ //checks to see if we have seen a word for this tag before
                    double Addto = observationsMap.get(tagList.get(k)).get(word) +1; //if we have, increment the amount of times we've seen a word with this tag by one
                    observationsMap.get(tagList.get(k)).put(word, Addto); //make the change
                }
                else{
                    //if we've seen the tag, but not the word for it yet, then start an observation count for that word
                    observationsMap.get(tagList.get(k)).put(word, (double) 1);
                }
            }
            else{
                Map<String, Double> wordCount = new HashMap<String, Double>(); //create a new map for new tag that holds words and counts
                wordCount.put(word, (double) 1); //starts a count for the word we are currently on in the list
                observationsMap.put(tagList.get(k),wordCount); //creates the table
            }
            k = k+1;
        }

        trainedTransitionsMap = new HashMap<>();
        trainedObservationsMap = new HashMap<>();

        for(String state: observationsMap.keySet()){
            //total for that state is initially zero
            double total = 0;
            //for all the words that match that state
            for(String word: observationsMap.get(state).keySet()){//first calculate total for each row
                total = total + observationsMap.get(state).get(word);
            }
            Map<String, Double> nextState = new HashMap<>();
            for(String word: observationsMap.get(state).keySet()){
                double probability = Math.log(observationsMap.get(state).get(word)/total); //calc. probability
                nextState.put(word, probability);
            }
            trainedObservationsMap.put(state, nextState); //store probability in training map
        }

        for(String state: transitionsMap.keySet()){
            double total = 0;

            for(String tag: transitionsMap.get(state).keySet()){
                total = total+ transitionsMap.get(state).get(tag);
            }
            Map<String, Double> nextState = new HashMap<>();
            for(String tag: transitionsMap.get(state).keySet()){
                double probability = Math.log(transitionsMap.get(state).get(tag)/total);
                nextState.put(tag, probability);
            }
            trainedTransitionsMap.put(state, nextState);
        }
    }

    public ArrayList<String> input(String testPath){
        ArrayList<String> inputtedWords = new ArrayList<String>();
        BufferedReader wordInput = null;

        try{
            wordInput = new BufferedReader(new FileReader(testPath));
            String line;
            while((line=wordInput.readLine()) != null){
                String wordsL = line.toLowerCase();
                String[] words = wordsL.split(" ");
                for(int i =1; i< words.length; i++){
                    inputtedWords.add((words[i]));
                }
            }
        }
        catch(IOException e){
            System.out.println(e);
        }
        try{
            wordInput.close();
        }
        catch (IOException e){
            System.out.println(e);
        }

        return inputtedWords;
    }

    public void viterbi(ArrayList<String> inputtedWords){
        //Psuedo Code Citation: Course Webpage -> Oct 30 -> Pattern Recognition
        ArrayList<Map<String,String>> backTrack = new ArrayList<Map<String,String>>();
        Set<String> currStates = new HashSet<>();
        currStates.add("#");
        Map<String, Double> currScores = new HashMap<>();
        currScores.put("#", 0.0);
        double penalty = -100.0;


        for(int j =0; j< inputtedWords.size()-1; j++){
            //make sure everything in set is also in curr scores
            Set<String> nextStates = new HashSet<>(); //look at all possible next states
            Map<String, String> stateTracker = new HashMap<>(); //track the highest score next state and curr state
            Map<String, Double> nextScores = new HashMap<>(); //store the scores for all possible next states
            for(String currState: currStates){
                for(String nextState: trainedTransitionsMap.get(currState).keySet()){
                    nextStates.add(nextState);
                    double nextScore;

                    if(!trainedObservationsMap.get(nextState).containsKey(inputtedWords.get(j+1))){
                        nextScore = currScores.get(currState) + trainedTransitionsMap.get(currState).get(nextState) + penalty;
                    }
                    else{
                        nextScore = currScores.get(currState) + trainedTransitionsMap.get(currState).get(nextState) + trainedObservationsMap.get(nextState).get(inputtedWords.get(j+1));
                    }

                    if(!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)){
                        nextScores.put(nextState, nextScore);
                        stateTracker.put(nextState, currState);
                        //remember that pred of nextState @ i is curr
                    }
                }
            }
            backTrack.add(stateTracker);
            currStates = nextStates;
            currScores = nextScores;
        }

        String mostProbable = null; //String that will hold the tag with highest score

        for(String currTag: currScores.keySet()){ //loop through all possible tags
            if(mostProbable == null){ //first time through the iteration set highest score to the first possible tag
                mostProbable = currTag;
            }else if(currScores.get(currTag) > currScores.get(mostProbable)){//if next tag score higher than previous
                mostProbable = currTag;//set that tag score as new highest score
            }
        }

        // follow its back pointers
        pathSeries = new ArrayList<String>(); //array list of the ordered tags
        String currhighScore = mostProbable;
        for(int i= backTrack.size()-1;i > -1;i--){ //start at back and hunt your way to the front of the list
            pathSeries.add(0, currhighScore); //add current tag with highest score (back most tag)
            currhighScore = backTrack.get(i).get(currhighScore); //set new highest score to next tag
        }
        for(int j = 0; j<pathSeries.size(); j++){
            if(pathSeries.get(j).equals(".") || pathSeries.get(j).equals("#")){//special common cases
                System.out.println(" ");
            }
            else{
                System.out.print(inputtedWords.get(j+1)+ "/" +pathSeries.get(j).toUpperCase() + " ");
            }
        }
    }

    public ArrayList<String> consoleTesting(SudiAssistant sudiChild) {
        ArrayList<String> userInput = new ArrayList<String>();
        System.out.println(" ");
        System.out.println("Enter Sentences Billiam:");
        Scanner in = new Scanner(System.in);
        while(true){ //stops when code is quit
            System.out.print("-->");
            String input = in.nextLine();
            String[] key = input.split(" "); //makes sentence into an array
            userInput.add("#");
            for(int i=0; i<key.length; i++){
                userInput.add(key[i]);
            }
            if(userInput.size()==0){
                System.out.println("No words detected:");
            }
            else{
                sudiChild.viterbi(userInput); //assigns tags to words and prints from viterbi algorithm
                userInput = new ArrayList<String>(); //allows for next input
                System.out.println(" ");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String tagPath = "C:\\Users\\glope\\Documents\\IdeaProjects\\cs10\\ps5\\brown-train-tags.txt";
        String sentencePath = "C:\\Users\\glope\\Documents\\IdeaProjects\\cs10\\ps5\\brown-train-sentences.txt";
        String sentencetestPath = "C:\\Users\\glope\\Documents\\IdeaProjects\\cs10\\ps5\\brown-test-sentences.txt";
        String tagtestPath = "C:\\Users\\glope\\Documents\\IdeaProjects\\cs10\\ps5\\brown-test-tags.txt";
        SudiAssistant Sudi = new SudiAssistant();
        ArrayList < String > Tags = Sudi.readFiletags(tagPath);
        ArrayList < String > Words = Sudi.readFileSentences(sentencePath);
        Sudi.trainMaps(Tags, Words);
        Sudi.viterbi(Sudi.input(sentencetestPath));

        SudiAssistant sudiChild = new SudiAssistant();
        ArrayList < String > TagsChild = sudiChild.readFiletags(tagPath);
        ArrayList< String > WordsChild = sudiChild.readFileSentences(sentencePath);
        sudiChild.trainMaps(TagsChild, WordsChild);
        sudiChild.consoleTesting(sudiChild);

    }
}
