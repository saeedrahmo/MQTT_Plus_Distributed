package MqttPlus.Syntax;

import MqttPlus.Operators.*;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class SyntaxAnalyzer {

    private static final String SLASH = "/";
    private static final String OPERATOR_SEPARATOR = "$";


    public static ArrayList<Operator> checkOperatorSyntax(String operationBlock) throws SyntaxException {
        if(!(operationBlock.startsWith(OPERATOR_SEPARATOR))) throw new SyntaxException();
        String[] operators = operationBlock.split(Pattern.quote(OPERATOR_SEPARATOR));
        ArrayList<Operator> arrayList = new ArrayList<>();

        int periodicCount = 0;

        for(int i = 1 ; i < operators.length; i++){
            String op = operators[i];
            Operator operator;
            if((operator = RuleBasedOperator.parse(op))!= null){
                arrayList.add(operator);
            }
            else if((operator = LastValueOperator.parse(op))!=null){
                arrayList.add(operator);
            }
            else if((operator = TemporalOperator.parse(op))!=null){
                arrayList.add(operator);
            }
            else if((operator = PeriodicOperator.parse(op))!=null){
                arrayList.add(operator);
                periodicCount++;
            }
            else if((operator=InformationExtractionOperator.parse(op))!=null){
                arrayList.add(operator);
            }
            else {
                throw new SyntaxException();
            }
        }

        if (periodicCount > 1) throw new SyntaxException();

        return arrayList;
    }

    public static void checkTopicSytax(String topic) throws SyntaxException{

    }

    public static ArrayList<String> tokenizeTopic(String topic){
        ArrayList<String> tokens = new ArrayList<>();
        for (String s : topic.split(SLASH)){
            tokens.add(s);
        }
        return tokens;
    }



}
