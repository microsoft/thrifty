/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.microsoft.thrifty.test.gen;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})

public class ListBonks implements org.apache.thrift.TBase<ListBonks, ListBonks._Fields>, java.io.Serializable, Cloneable, Comparable<ListBonks> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ListBonks");

  private static final org.apache.thrift.protocol.TField BONK_FIELD_DESC = new org.apache.thrift.protocol.TField("bonk", org.apache.thrift.protocol.TType.LIST, (short)1);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new ListBonksStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new ListBonksTupleSchemeFactory();

  public @org.apache.thrift.annotation.Nullable java.util.List<Bonk> bonk; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    BONK((short)1, "bonk");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // BONK
          return BONK;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.BONK, new org.apache.thrift.meta_data.FieldMetaData("bonk", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Bonk.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ListBonks.class, metaDataMap);
  }

  public ListBonks() {
  }

  public ListBonks(
    java.util.List<Bonk> bonk)
  {
    this();
    this.bonk = bonk;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ListBonks(ListBonks other) {
    if (other.isSetBonk()) {
      java.util.List<Bonk> __this__bonk = new java.util.ArrayList<Bonk>(other.bonk.size());
      for (Bonk other_element : other.bonk) {
        __this__bonk.add(new Bonk(other_element));
      }
      this.bonk = __this__bonk;
    }
  }

  public ListBonks deepCopy() {
    return new ListBonks(this);
  }

  @Override
  public void clear() {
    this.bonk = null;
  }

  public int getBonkSize() {
    return (this.bonk == null) ? 0 : this.bonk.size();
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.Iterator<Bonk> getBonkIterator() {
    return (this.bonk == null) ? null : this.bonk.iterator();
  }

  public void addToBonk(Bonk elem) {
    if (this.bonk == null) {
      this.bonk = new java.util.ArrayList<Bonk>();
    }
    this.bonk.add(elem);
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.List<Bonk> getBonk() {
    return this.bonk;
  }

  public ListBonks setBonk(@org.apache.thrift.annotation.Nullable java.util.List<Bonk> bonk) {
    this.bonk = bonk;
    return this;
  }

  public void unsetBonk() {
    this.bonk = null;
  }

  /** Returns true if field bonk is set (has been assigned a value) and false otherwise */
  public boolean isSetBonk() {
    return this.bonk != null;
  }

  public void setBonkIsSet(boolean value) {
    if (!value) {
      this.bonk = null;
    }
  }

  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
    switch (field) {
    case BONK:
      if (value == null) {
        unsetBonk();
      } else {
        setBonk((java.util.List<Bonk>)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case BONK:
      return getBonk();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case BONK:
      return isSetBonk();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof ListBonks)
      return this.equals((ListBonks)that);
    return false;
  }

  public boolean equals(ListBonks that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_bonk = true && this.isSetBonk();
    boolean that_present_bonk = true && that.isSetBonk();
    if (this_present_bonk || that_present_bonk) {
      if (!(this_present_bonk && that_present_bonk))
        return false;
      if (!this.bonk.equals(that.bonk))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetBonk()) ? 131071 : 524287);
    if (isSetBonk())
      hashCode = hashCode * 8191 + bonk.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(ListBonks other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetBonk()).compareTo(other.isSetBonk());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetBonk()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.bonk, other.bonk);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  @org.apache.thrift.annotation.Nullable
  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("ListBonks(");
    boolean first = true;

    sb.append("bonk:");
    if (this.bonk == null) {
      sb.append("null");
    } else {
      sb.append(this.bonk);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class ListBonksStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ListBonksStandardScheme getScheme() {
      return new ListBonksStandardScheme();
    }
  }

  private static class ListBonksStandardScheme extends org.apache.thrift.scheme.StandardScheme<ListBonks> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ListBonks struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // BONK
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list256 = iprot.readListBegin();
                struct.bonk = new java.util.ArrayList<Bonk>(_list256.size);
                @org.apache.thrift.annotation.Nullable Bonk _elem257;
                for (int _i258 = 0; _i258 < _list256.size; ++_i258)
                {
                  _elem257 = new Bonk();
                  _elem257.read(iprot);
                  struct.bonk.add(_elem257);
                }
                iprot.readListEnd();
              }
              struct.setBonkIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, ListBonks struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.bonk != null) {
        oprot.writeFieldBegin(BONK_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.bonk.size()));
          for (Bonk _iter259 : struct.bonk)
          {
            _iter259.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ListBonksTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ListBonksTupleScheme getScheme() {
      return new ListBonksTupleScheme();
    }
  }

  private static class ListBonksTupleScheme extends org.apache.thrift.scheme.TupleScheme<ListBonks> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ListBonks struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetBonk()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetBonk()) {
        {
          oprot.writeI32(struct.bonk.size());
          for (Bonk _iter260 : struct.bonk)
          {
            _iter260.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ListBonks struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list261 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.bonk = new java.util.ArrayList<Bonk>(_list261.size);
          @org.apache.thrift.annotation.Nullable Bonk _elem262;
          for (int _i263 = 0; _i263 < _list261.size; ++_i263)
          {
            _elem262 = new Bonk();
            _elem262.read(iprot);
            struct.bonk.add(_elem262);
          }
        }
        struct.setBonkIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

