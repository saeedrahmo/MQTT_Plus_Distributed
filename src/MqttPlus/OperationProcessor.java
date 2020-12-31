package MqttPlus;

import MqttPlus.Publish.Publish;
import MqttPlus.PublishBuffers.*;
import MqttPlus.Operators.*;
import MqttPlus.Subscription.Subscription;
import MqttPlus.enums.LastValueOperatorEnum;
import MqttPlus.enums.RuleBasedOperatorEnum;

import java.util.ArrayList;
import java.util.Collections;

public class OperationProcessor {

    public static Publish periodicSubsriptionProcess(Subscription subscription){

        ArrayList<Operator> operators = new ArrayList<>(subscription.getOperators());
        Collections.reverse(operators);

        PublishBuffer publishBuffer = PublishBuffer.chooseRightBuffer(operators.get(0));

        PeriodicOperator periodicOperator = subscription.getPeriodicOperator();

        Publish newPublish = new Publish(null,subscription.getCompleteTopic(),null);

        newPublish.setPayload(periodicOperation(periodicOperator,subscription,publishBuffer));

        if(operators.get(0).getClass() == InformationExtractionOperator.class){
            operators.remove(1);
            operators.remove(0);
        }
        else{
            operators.remove(0);
        }


        return recursivePeriodic(operators,newPublish,subscription);
    }


    public static Publish subcriptionProcess(Publish originalPublish, Subscription subscription){

        ArrayList<Operator> operators = new ArrayList<>(subscription.getOperators());
        Collections.reverse(operators);

        Publish publish = new Publish(originalPublish);

        if(operators.get(0).getClass() == InformationExtractionOperator.class){
            if(operators.size() > 1 && operators.get(1).getClass()==TemporalOperator.class){
                publish.setPayload(temporalOperation(operators.get(1),subscription, PublishBuffer.chooseRightBuffer(operators.get(0))));
                operators.remove(1);
                operators.remove(0);
            }
            else if(operators.size() > 1 && operators.get(1).getClass()==LastValueOperator.class){
                publish.setPayload(lastValueOperation(operators.get(1),subscription, PublishBuffer.chooseRightBuffer(operators.get(0))));
                operators.remove(1);
                operators.remove(0);
            }
            else {
                publish.setPayload(informationExtractorOperator(operators.get(0),originalPublish));
                operators.remove(0);
            }

        }

        publish.setTopic(subscription.getCompleteTopic());

        return recursive(operators,publish,subscription);


    }

    private static ArrayList<Double> fiterValues(Operator operator, Object payload){
        ArrayList<Double> values = null;
        try {
             values = (ArrayList<Double> )payload;
        }
        catch (ClassCastException e){
            try {
                Double value = (Double) payload;
                values = new ArrayList<>();
                values.add(value);
            }
            catch (ClassCastException e1){
                return null;
            }
        }

        ArrayList<Double> filteredValues = new ArrayList<>();
        for (Double value : values){
            if(ruleBaseOperation(operator,value)){
                filteredValues.add(value);
            }
        }
        if (filteredValues.isEmpty()) return null;

        return filteredValues;
    }

    public static Publish recursivePeriodic(ArrayList<Operator> operatorArrayList, Publish publish, Subscription subscription){
        if(!operatorArrayList.isEmpty() && publish.getPayload()!=null){
            Operator op = operatorArrayList.get(0);
            if(op.getClass()==RuleBasedOperator.class){
                publish.setPayload(fiterValues(op,publish.getPayload()));
            }
            else if(op.getClass() == LastValueOperator.class){
                publish.setPayload(aggregationOperation(op,publish.getPayload()));
            }
            else {
                publish.setPayload(null);
            }
            operatorArrayList.remove(0);
            return recursivePeriodic(operatorArrayList,publish,subscription);
        }
        else {
            return publish;
        }

    }

    public static Publish recursive(ArrayList<Operator> operatorArrayList, Publish publish, Subscription subscription){
        if(!operatorArrayList.isEmpty() && publish.getPayload()!=null) {
            Operator op = operatorArrayList.get(0);
            if(op.getClass() == RuleBasedOperator.class){
                if(!ruleBaseOperation(op,publish.getPayload())){
                    publish.setPayload(null);
                }
            }
            else if(op.getClass() == LastValueOperator.class){
                publish.setPayload(lastValueOperation(op,subscription, PublishBuffer.chooseRightBuffer(op)));
            }
            else if(op.getClass() == TemporalOperator.class){
                publish.setPayload(temporalOperation(op,subscription, PublishBuffer.chooseRightBuffer(op)));
            }
            else if(op.getClass() == InformationExtractionOperator.class){
                publish.setPayload(informationExtractorOperator(op,publish));
            }
            operatorArrayList.remove(0);
            return recursive(operatorArrayList,publish,subscription);
        }
        else {
            return publish;
        }
    }

    private static Double informationExtractorOperator(Operator op, Publish publish){
        PublishBuffer publishBuffer = PublishBuffer.chooseRightBuffer(op);
        return publishBuffer.getLastValue(publish.getTopic());

    }

    private static ArrayList<Double> periodicOperation(PeriodicOperator periodicOperator, Subscription subscription, PublishBuffer publishBuffer){

        ArrayList<Double> matches = null;

        switch (periodicOperator.getLastValueOperatorEnum()){
            case AVG:
                matches = publishBuffer.getMatchingValue(subscription.getTopic(),LastValueOperatorEnum.AVG,periodicOperator.getPeriodicOperatorEnum());
                break;
            case SUM:
                matches = publishBuffer.getMatchingValue(subscription.getTopic(),periodicOperator.getLastValueOperatorEnum(),periodicOperator.getPeriodicOperatorEnum());
                break;
            case COUNT:
                matches = publishBuffer.getMatchingValue(subscription.getTopic(),periodicOperator.getLastValueOperatorEnum(),periodicOperator.getPeriodicOperatorEnum());
                break;
            case MAX:
                matches = publishBuffer.getMatchingValue(subscription.getTopic(),periodicOperator.getLastValueOperatorEnum(),periodicOperator.getPeriodicOperatorEnum());
                break;
            case MIN:
                matches = publishBuffer.getMatchingValue(subscription.getTopic(),periodicOperator.getLastValueOperatorEnum(),periodicOperator.getPeriodicOperatorEnum());
                break;
        }
        if(matches.isEmpty()) return null;

        return matches;
    }

    private static Double temporalOperation(Operator op, Subscription subscription, PublishBuffer publishBuffer){
        TemporalOperator operator = (TemporalOperator) op;

        ArrayList matches = publishBuffer.getMatchingTemporalBuffer(subscription.getTopic(),operator.getTimeSpan());

        switch (operator.getTemporalOperatorEnum()){
            case TMPAVG:
                return computeAvg(matches);
            case TMPSUM:
                return computeSum(matches);
            case TMPCOUNT:
                return new Double(matches.size());
            case TMPMAX:
                return computeMax(matches);
            case TMPMIN:
                return computeMin(matches);
        }
        return null;
    }

    private static Double lastValueOperation(Operator op, Subscription subscription,PublishBuffer buffer){
        LastValueOperator operator = (LastValueOperator) op;
        ArrayList<Double> matches = buffer.getMatchingValue(subscription.getTopic(),operator.getLastValueOperator(),null);
        return aggregationOperation(op,matches);
    }

    private static Double aggregationOperation(Operator op, Object matches){
        LastValueOperator operator = (LastValueOperator) op;
        ArrayList<Double> values = (ArrayList<Double>) matches;
        switch (operator.getLastValueOperator()){
            case AVG:
                return computeAvg(values);
            case SUM:
                return computeSum(values);
            case COUNT:
                return new Double(values.size());
            case MAX:
                return computeMax(values);
            case MIN:
                return computeMin(values);
            default:
                return null;
        }

    }

    private static Double computeAvg(ArrayList<Double> matches){
        return computeSum(matches)/matches.size();
    }

    private static Double computePeriodicAvg(ArrayList<Double> matches){
        return computeSum(matches)/computeCount(matches);
    }
    private static Double computeSum(ArrayList<Double> values){
        Double sum = Double.NaN;
        for (Double d : values){
            if(sum.isNaN()&& !d.isNaN()){
                sum = d;
            }
            else if(!sum.isNaN() && !d.isNaN()) {
                sum += d;
            }

        }
        return sum;
    }
    private static Double computeCount(ArrayList<Double> values){
        Double count = Double.NaN;
        for (Double d : values){
            if(count.isNaN()&& !d.isNaN()){
                count = d;
            }
            else if(!count.isNaN() && !d.isNaN()) {
                count += d;
            }
        }
        return count;
    }
    private static Double computeMax(ArrayList<Double> values){
        values.removeIf(n -> n.isNaN());
        if(values.isEmpty()) return Double.NaN;
        return Collections.max(values);
    }
    private static Double computeMin(ArrayList<Double> values){
        values.removeIf(n -> n.isNaN());
        if(values.isEmpty()) return Double.NaN;
        return Collections.min(values);
    }

    private static boolean ruleBaseOperation(Operator op,Object value){
        RuleBasedOperator operator = (RuleBasedOperator) op;

        if (operator.getRuleBasedOperator() == RuleBasedOperatorEnum.CONTAINS){
            String strValue = value.toString();
            if (strValue.contains(operator.getValue().toString())) return true;
            else return false;
        }
        try {
            Double dValue = new Double(value.toString());
            Double opValue = new Double(operator.getValue().toString());

            switch (operator.getRuleBasedOperator()) {
                case GT:
                    return dValue > opValue;
                case GTE:
                    return dValue >= opValue;
                case LT:
                    return dValue < opValue;
                case LTE:
                    return dValue <= opValue;
                case EQ:
                    return dValue.equals(opValue);
                case NEQ:
                    return !dValue.equals(opValue);
                default:
                    return false;
            }
        }catch (NumberFormatException e){
            return false;
        }

    }
}
