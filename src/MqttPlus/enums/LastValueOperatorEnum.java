package MqttPlus.enums;

public enum LastValueOperatorEnum {
    AVG,
    SUM,
    MIN,
    MAX,
    COUNT;


    public String toString(){
        String string;
        switch (this) {
            case AVG:
                string = "AVG";
                break;
            case SUM:
                string = "SUM";
                break;
            case MIN:
                string = "MIN";
                break;
            case MAX:
                string = "MAX";
                break;
            case COUNT:
                string = "COUNT";
                break;
            default:
                string = "";
                break;
        }
        return string;
    }


    public static LastValueOperatorEnum fromString(String string){
        LastValueOperatorEnum op;
        switch (string){
            case "AVG":
                op = AVG;
                break;
            case "SUM":
                op = SUM;
                break;
            case "MAX":
                op = MAX;
                break;
            case "MIN":
                op = MIN;
                break;
            case "COUNT":
                op = COUNT;
                break;
            default:
                op = null;
        }
        return op;
    }


}
