package MqttPlus.Subscription;

import MqttPlus.Operators.InformationExtractionOperator;
import MqttPlus.Operators.Operator;
import MqttPlus.Operators.PeriodicOperator;
import MqttPlus.Operators.TemporalOperator;
import MqttPlus.Sematic.SemanticAnalyzer;
import MqttPlus.Sematic.SemanticException;
import MqttPlus.Syntax.SyntaxAnalyzer;
import MqttPlus.Syntax.SyntaxException;
import MqttPlus.enums.InformationExtractionOperatorEnum;

import java.util.ArrayList;
import java.util.Objects;

public class Subscription {

    private static final String TOPIC_SEPARATOR = "/";
    private static final String OP_SEPARATOR = "$";

    private String topic;
    private ArrayList<Operator> operators;
    private boolean periodic = false;
    private PeriodicOperator periodicOperator = null;

    public Subscription(String topic, ArrayList<Operator> operators) {
        this.topic = topic;
        this.operators = operators;
        for (Operator op : operators){
            if(op.getClass()== PeriodicOperator.class){
                this.periodic = true;
                periodicOperator = (PeriodicOperator) op;
            }
        }
    }


    public ArrayList<Operator> getOperators() {
        return operators;
    }

    @Override
    public String toString() {
        return "{" + getCompleteTopic() + "}";
    }

    public String getTopic() {
        return topic;
    }

    public boolean isPeriodic() {
        return periodic;
    }

    public boolean isTemporal(){
        for (Operator op : operators){
            if(op.getClass()== TemporalOperator.class) return true;
        }
        return false;
    }

    public TemporalOperator getTemporalOperator(){
        for(Operator op : operators){
            if(op.getClass() ==  TemporalOperator.class) return (TemporalOperator)op;
        }
        return null;
    }


    public PeriodicOperator getPeriodicOperator(){
        return periodicOperator;
    }

    public String getCompleteTopic(){
        String text = "";
        for (Operator op : operators){
            text += OP_SEPARATOR + op.toString();
        }
        text += TOPIC_SEPARATOR + topic;
        return text;
    }

    public ArrayList<String> getTopicTokens(){
        String matchingPart = topic.substring(topic.indexOf(TOPIC_SEPARATOR));
        ArrayList<String> tokens = new ArrayList<>();
        for (String s : matchingPart.split(TOPIC_SEPARATOR)){
            tokens.add(s);
        }
        return tokens;
    }

    public static Subscription parse(String topic) throws SyntaxException, SemanticException, ParsingException {
        String operationBlock;
        String matchingPart;
        try {
            int firstSlash = topic.indexOf(TOPIC_SEPARATOR);
            operationBlock = topic.substring(0,firstSlash);
            matchingPart = topic.substring(firstSlash + 1);
        }
        catch (Exception e){
            e.printStackTrace();
            throw new ParsingException();
        }


        ArrayList<Operator> operators = SyntaxAnalyzer.checkOperatorSyntax(operationBlock);
        SyntaxAnalyzer.checkTopicSytax(matchingPart);
        SemanticAnalyzer.checkSemantic(operators);
        return new Subscription(matchingPart,operators);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;

        return this.toString().equals(that.toString());

    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, operators);
    }

    public boolean containsOperator(InformationExtractionOperatorEnum operator){
        for (Operator op : operators){
            if(op.getClass() == InformationExtractionOperator.class){
                if(((InformationExtractionOperator) op).getInformationExtractionOperatorEnum() == operator )return true;
            }
        }
        return false;
    }
}
