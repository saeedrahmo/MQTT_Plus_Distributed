package MqttPlus.Operators;

import MqttPlus.enums.DataType;
import MqttPlus.enums.RuleBasedOperatorEnum;

public class RuleBasedOperator extends Operator{

    private static final String SEMICOLON = ";";

    private final DataType inputDataType;
    private final DataType outputDataType;

    private RuleBasedOperatorEnum ruleBasedOperator;
    private Object value;

    public RuleBasedOperator(String string){

        String[] tokens = string.split(SEMICOLON);

        this.value = tokens[1];

        ruleBasedOperator = RuleBasedOperatorEnum.fromString(tokens[0]);
        if(ruleBasedOperator == RuleBasedOperatorEnum.CONTAINS){
            inputDataType = DataType.STRING;
            outputDataType = DataType.STRING;
        }
        else  {
            inputDataType = DataType.OPTIONALNUMERICSET;
            outputDataType = DataType.OPTIONALNUMERICSET;
        }

    }

    public DataType getInputDataType() {
        return inputDataType;
    }

    public DataType getOutputDataType() {
        return outputDataType;
    }

    public RuleBasedOperatorEnum getRuleBasedOperator() {
        return ruleBasedOperator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return ruleBasedOperator.getOperator() + SEMICOLON + value.toString();
    }

    public static RuleBasedOperator parse(String string){
        if(isValidOperator(string)){
            return new RuleBasedOperator(string);
        }
        else {
            return null;
        }
    }

    public static boolean isValidOperator(String operator){
        for(RuleBasedOperatorEnum op : RuleBasedOperatorEnum.values()){
            if (op == RuleBasedOperatorEnum.CONTAINS){
                if(operator.matches(op.getOperator() + SEMICOLON + ".+$")) return true;
            }
            else {
                if (operator.matches( op.getOperator() + SEMICOLON + "[-+]?([0-9]*\\.[0-9]+|[0-9]+)$")) return true;
            }
        }
        return false;
    }


}
