package MqttPlus.Operators;

import MqttPlus.enums.DataType;

public abstract class Operator {

    public abstract DataType getInputDataType();

    public abstract DataType getOutputDataType();
}
