package MqttPlus.Operators;

import MqttPlus.enums.DataType;
import MqttPlus.enums.InformationExtractionOperatorEnum;

public class InformationExtractionOperator extends Operator {

    private InformationExtractionOperatorEnum informationExtractionOperatorEnum;

    private final DataType inputDataType;
    private final DataType outputDataType;

    public InformationExtractionOperator(String string){
        inputDataType = DataType.IMAGEBUFFER;
        informationExtractionOperatorEnum = InformationExtractionOperatorEnum.fromString(string);
        if(string.equals(InformationExtractionOperatorEnum.COUNTPEOPLE)){
            outputDataType = DataType.PEOPLECOUNT;
        }
        else if(string.equals(InformationExtractionOperatorEnum.COUNTMALE)){
            outputDataType = DataType.MALECOUNT;
        }
        else {
            outputDataType = DataType.FEMALECOUNT;
        }
    }

    public DataType getInputDataType() {
        return inputDataType;
    }

    public DataType getOutputDataType() {
        return outputDataType;
    }

    public InformationExtractionOperatorEnum getInformationExtractionOperatorEnum() {
        return informationExtractionOperatorEnum;
    }

    public static boolean isValidOperator(String operator){
        for (InformationExtractionOperatorEnum op : InformationExtractionOperatorEnum.values()){
            if(operator.matches(op.toString())) return true;
        }
        return false;
    }

    public static InformationExtractionOperator parse(String string){
        if (isValidOperator(string)){
            return new InformationExtractionOperator(string);
        }
        else return null;
    }

    @Override
    public String toString() {
        return informationExtractionOperatorEnum.toString();
    }
}
