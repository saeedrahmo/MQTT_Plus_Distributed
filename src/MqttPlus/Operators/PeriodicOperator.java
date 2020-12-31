package MqttPlus.Operators;

import MqttPlus.enums.DataType;
import MqttPlus.enums.LastValueOperatorEnum;
import MqttPlus.enums.PeriodicOperatorEnum;

public class PeriodicOperator extends Operator{

    private PeriodicOperatorEnum periodicOperatorEnum;
    private LastValueOperatorEnum lastValueOperatorEnum;
    private final DataType inputDataType = DataType.ANYTHING;
    private final DataType outputDataType = DataType.NUMERICSET;

    public PeriodicOperator(String periodicOperator, String lastValueOperator){
        periodicOperatorEnum = PeriodicOperatorEnum.fromString(periodicOperator);
        lastValueOperatorEnum = LastValueOperatorEnum.fromString(lastValueOperator);
    }

    public DataType getInputDataType() {
        return inputDataType;
    }

    public DataType getOutputDataType() {
        return outputDataType;
    }

    public PeriodicOperatorEnum getPeriodicOperatorEnum() {
        return periodicOperatorEnum;
    }


    public LastValueOperatorEnum getLastValueOperatorEnum() {
        return lastValueOperatorEnum;
    }

    public static PeriodicOperator parse(String string){
        if(isValidOperator(string)){
            String lastValueOperator = string.split("(DAILY|HOURLY|QUARTERHOURLY)")[1];
            String periodicOperator = string.split(lastValueOperator)[0];
            return new PeriodicOperator(periodicOperator,lastValueOperator);
        }
        else return null;
    }

    public static boolean isValidOperator(String operator){
        for (PeriodicOperatorEnum periodicOperator : PeriodicOperatorEnum.values()){
            for(LastValueOperatorEnum lastValueOperator : LastValueOperatorEnum.values())
                if(operator.matches(periodicOperator.getOperator() + lastValueOperator.toString())) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return periodicOperatorEnum.toString() + lastValueOperatorEnum.toString() ;
    }
}
