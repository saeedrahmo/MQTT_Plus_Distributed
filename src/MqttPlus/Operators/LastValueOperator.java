package MqttPlus.Operators;

import MqttPlus.enums.DataType;
import MqttPlus.enums.LastValueOperatorEnum;

public class LastValueOperator extends Operator{

    private LastValueOperatorEnum lastValueOperator;

    private final DataType inputDataType = DataType.OPTIONALNUMERICSET;
    private final DataType outputDataType = DataType.NUMERIC;

    public LastValueOperator(String operator){
        lastValueOperator = LastValueOperatorEnum.fromString(operator);
    }

    public DataType getInputDataType() {
        return inputDataType;
    }

    public DataType getOutputDataType() {
        return outputDataType;
    }

    public LastValueOperatorEnum getLastValueOperator() {
        return lastValueOperator;
    }

    public static boolean isValidOperator(String operator){
        for (LastValueOperatorEnum op : LastValueOperatorEnum.values()){
            if(operator.matches(op.toString())) return true;
        }
        return false;
    }

    public static LastValueOperator parse(String string){
        if(isValidOperator(string)){
            return new LastValueOperator(string);
        }
        else return null;
    }

    @Override
    public String toString() {
        return lastValueOperator.toString();
    }
}
