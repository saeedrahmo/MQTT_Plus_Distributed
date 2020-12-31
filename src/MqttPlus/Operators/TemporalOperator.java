package MqttPlus.Operators;

import MqttPlus.enums.DataType;
import MqttPlus.enums.TemporalOperatorEnum;
import MqttPlus.enums.TimeSpan;

public class TemporalOperator extends Operator {

    private TemporalOperatorEnum temporalOperatorEnum;

    private static final String SEMICOLON = ";";
    private final DataType inputDataType = DataType.ANYTHING;
    private final DataType outputDataType = DataType.NUMERIC;

    private TimeSpan timeSpan;

    public TemporalOperator(String string){
        String tokens[] = string.split(SEMICOLON);
        temporalOperatorEnum = TemporalOperatorEnum.fromString(tokens[0]);
        timeSpan = new TimeSpan(tokens[1]);
    }

    public DataType getInputDataType() {
        return inputDataType;
    }

    public DataType getOutputDataType() {
        return outputDataType;
    }

    public TemporalOperatorEnum getTemporalOperatorEnum() {
        return temporalOperatorEnum;
    }

    public TimeSpan getTimeSpan() {
        return timeSpan;
    }

    public static boolean isValidOperator(String operator){
        for (TemporalOperatorEnum op : TemporalOperatorEnum.values()){
            if(operator.matches(op.getOperator() + ";([0-9]|0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")) return true;
        }
        return false;
    }

    public static TemporalOperator parse(String string){
        if(isValidOperator(string)){
            return new TemporalOperator(string);
        }
        else return null;
    }

    @Override
    public String toString() {
        return temporalOperatorEnum.toString() + SEMICOLON + timeSpan.toString();
    }
}
