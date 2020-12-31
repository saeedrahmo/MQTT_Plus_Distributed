package MqttPlus.enums;

public enum InformationExtractionOperatorEnum {
    COUNTPEOPLE,
    COUNTMALE,
    COUNTFEMALE;


    public String toString(){
        String string;
        switch (this) {
            case COUNTPEOPLE:
                string = "COUNTPEOPLE";
                break;
            case COUNTMALE:
                string = "COUNTMALE";
                break;
            case COUNTFEMALE:
                string = "COUNTFEMALE";
                break;
            default:
                string = "";
                break;
        }
        return string;
    }

    public static InformationExtractionOperatorEnum fromString(String string){
        InformationExtractionOperatorEnum op;
        switch (string){
            case "COUNTPEOPLE":
                op = COUNTPEOPLE;
                break;
            case "COUNTMALE":
                op = COUNTMALE;
                break;
            case "COUNTFEMALE":
                op = COUNTFEMALE;
                break;
            default:
                op = null;
        }
        return op;
    }
}
