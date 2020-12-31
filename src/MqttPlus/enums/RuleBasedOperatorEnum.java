package MqttPlus.enums;

public enum RuleBasedOperatorEnum {
    GT,
    GTE,
    LT,
    LTE,
    EQ,
    NEQ,
    CONTAINS;

    public String getOperator(){
        String string;
        switch (this) {
            case GT:
                string = "GT";
                break;
            case GTE:
                string = "GTE";
                break;
            case LT:
                string = "LT";
                break;
            case LTE:
                string = "LTE";
                break;
            case EQ:
                string = "EQ";
                break;
            case NEQ:
                string = "NEQ";
                break;
            case CONTAINS:
                string = "CONTAINS";
                break;
            default:
                string = "";
                break;
        }
        return string;
    }


    public static RuleBasedOperatorEnum fromString(String string){
        RuleBasedOperatorEnum op;
        switch (string){
            case "GT":
                op = GT;
                break;
            case "GTE":
                op = GTE;
                break;
            case "LT":
                op = LT;
                break;
            case "LTE":
                op = LTE;
                break;
            case "EQ":
                op = EQ;
                break;
            case "NEQ":
                op = NEQ;
                break;
            case "CONTAINS":
                op = CONTAINS;
                break;
            default:
                op = null;
        }

        return op;

    }


}
