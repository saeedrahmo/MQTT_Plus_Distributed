package MqttPlus.enums;



public enum TemporalOperatorEnum {

    TMPAVG,
    TMPSUM,
    TMPMIN,
    TMPMAX,
    TMPCOUNT;


    public String getOperator(){
        String string;
        switch (this) {
            case TMPAVG:
                string = "TMPAVG";
                break;
            case TMPSUM:
                string = "TMPSUM";
                break;
            case TMPMIN:
                string = "TMPMIN";
                break;
            case TMPMAX:
                string = "TMPMAX";
                break;
            case TMPCOUNT:
                string = "TMPCOUNT";
                break;
            default:
                string = "";
                break;
        }
        return string;
    }


    public static TemporalOperatorEnum fromString(String operator){
        TemporalOperatorEnum op;

        switch (operator){
            case "TMPAVG":
                op = TMPAVG;
                break;
            case "TMPSUM":
                op = TMPSUM;
                break;
            case "TMPMIN":
                op = TMPMIN;
                break;
            case "TMPMAX":
                op = TMPMAX;
                break;
            case "TMPCOUNT":
                op = TMPCOUNT;
                break;
            default:
                op = null;
        }
        return op;
    }
}
