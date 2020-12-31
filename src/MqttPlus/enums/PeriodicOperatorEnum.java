package MqttPlus.enums;

public enum PeriodicOperatorEnum {
    DAILY,
    HOURLY,
    QUARTERHOURLY;


    public String getOperator(){
        String string;
        switch (this) {
            case DAILY:
                string = "DAILY";
                break;
            case HOURLY:
                string = "HOURLY";
                break;
            case QUARTERHOURLY:
                string = "QUARTERHOURLY";
                break;
            default:
                string = "";
                break;
        }
        return string;
    }

    public static PeriodicOperatorEnum fromString(String operator){
        PeriodicOperatorEnum op ;

        switch (operator){
            case "DAILY":
                op = DAILY;
                break;
            case "HOURLY":
                op = HOURLY;
                break;
            case "QUARTERHOURLY":
                op = QUARTERHOURLY;
                break;
            default:
                op = null;
        }

        return op;
    }


}
