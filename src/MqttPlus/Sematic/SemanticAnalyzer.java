package MqttPlus.Sematic;

import MqttPlus.Operators.Operator;
import MqttPlus.enums.DataType;


import java.util.ArrayList;
import java.util.Collections;

public class SemanticAnalyzer {

    public static DataType checkSemantic(ArrayList<Operator> operatorArrayList) throws SemanticException{

        ArrayList<Operator> operators = new ArrayList<>(operatorArrayList);
        Collections.reverse(operators);

        DataType input = operators.get(0).getInputDataType();
        DataType opInput = null;
        DataType opOutput = null;

        for (int i = 0;i<operators.size();i++){
            opInput = operators.get(i).getInputDataType();
            opOutput = operators.get(i).getOutputDataType();

            if(!(isCompatible(input,opInput)||isSetCompatible(input,opInput)||isInnerCompatible(input,opInput))) throw new SemanticException();

            if(!isCompatible(input,opOutput)){
                if(isInnerCompatible(input,opOutput)){
                    input = input.getSetDT();
                }
                else {
                    input = opOutput;
                }
            }

        }
        return opOutput;
    }

    private static boolean isCompatible(DataType type1, DataType type2){

        if(type1 == type2) return true;
        else if(type2 == DataType.ANYTHING) return true;
        else if(type1 == DataType.IMAGEBUFFER && type2 == DataType.BYTEBUFFER) return true;
        else if(type1 == DataType.PEOPLECOUNT && type2 == DataType.NUMERIC) return true;
        else if(type1 == DataType.MALECOUNT && type2 == DataType.NUMERIC) return true;
        else if(type1 == DataType.FEMALECOUNT && type2 == DataType.NUMERIC) return true;
        else if(type1 == DataType.MALECOUNT && type2 == DataType.PEOPLECOUNT) return true;
        else if(type1 == DataType.FEMALECOUNT && type2 == DataType.PEOPLECOUNT) return true;
        else return false;

    }

    private static boolean isSetCompatible(DataType type1,DataType type2){

        if(type1 == type2) return true;
        else if(type2 == DataType.ANYTHING) return true;
        else if(type1 == DataType.NUMERICSET && type2 == DataType.OPTIONALNUMERICSET) return true;
        else if(type1 == DataType.PEOPLECOUNTSET && type2 == DataType.OPTIONALNUMERICSET) return true;
        else if(type1 == DataType.MALECOUNTSET && type2 == DataType.OPTIONALNUMERICSET) return true;
        else if(type1 == DataType.FEMALECOUNT && type2 == DataType.OPTIONALNUMERICSET) return true;
        else if(type1 == DataType.PEOPLECOUNTSET && type2 == DataType.NUMERICSET) return true;
        else if(type1 == DataType.MALECOUNTSET && type2 == DataType.NUMERICSET) return true;
        else if(type1 == DataType.FEMALECOUNTSET && type2 == DataType.NUMERICSET) return true;
        else if(type1 == DataType.MALECOUNTSET && type2 == DataType.PEOPLECOUNTSET) return true;
        else if(type1 == DataType.FEMALECOUNTSET && type2 == DataType.PEOPLECOUNTSET) return true;
        else return false;
    }


    private static boolean isInnerCompatible(DataType type1,DataType type2){
        return isCompatible(type1,type2.getInnerDT()) ? true : false;
    }


}
