package MqttPlus.enums;

public enum DataType {
    ANYTHING,
    BYTEBUFFER,
    IMAGEBUFFER,
    NUMERIC,
    PEOPLECOUNT,
    MALECOUNT,
    FEMALECOUNT,
    STRING,
    OPTIONALNUMERICSET,
    NUMERICSET,
    PEOPLECOUNTSET,
    MALECOUNTSET,
    FEMALECOUNTSET;


    public DataType getInnerDT(){
        switch (this){
            case OPTIONALNUMERICSET:
                return NUMERIC;
            case NUMERICSET:
                return NUMERIC;
            case PEOPLECOUNTSET:
                return PEOPLECOUNT;
            case MALECOUNTSET:
                return MALECOUNT;
            case FEMALECOUNTSET:
                return FEMALECOUNT;
            default:
                return this;
        }

    }

    public DataType getSetDT(){
        switch (this){
            case NUMERIC:
                return OPTIONALNUMERICSET;
            case PEOPLECOUNT:
                return PEOPLECOUNTSET;
            case FEMALECOUNT:
                return FEMALECOUNTSET;
            case MALECOUNT:
                return MALECOUNTSET;
            default:
                return null;
        }
    }
}
