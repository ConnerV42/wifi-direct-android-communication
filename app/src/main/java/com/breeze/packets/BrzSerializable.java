package com.breeze.packets;

public interface BrzSerializable {
  public String toJSON();
  public void fromJSON(String json);
}
