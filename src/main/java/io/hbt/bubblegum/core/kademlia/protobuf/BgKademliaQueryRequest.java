// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: BgKademliaQueryRequest.proto

package io.hbt.bubblegum.core.kademlia.protobuf;

public final class BgKademliaQueryRequest {
  private BgKademliaQueryRequest() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface KademliaQueryRequestOrBuilder extends
      // @@protoc_insertion_point(interface_extends:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * Times
     * </pre>
     *
     * <code>int64 fromTime = 1;</code>
     */
    long getFromTime();

    /**
     * <code>int64 toTime = 2;</code>
     */
    long getToTime();

    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    java.util.List<java.lang.String>
        getIdListList();
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    int getIdListCount();
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    java.lang.String getIdList(int index);
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    com.google.protobuf.ByteString
        getIdListBytes(int index);
  }
  /**
   * Protobuf type {@code io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest}
   */
  public  static final class KademliaQueryRequest extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest)
      KademliaQueryRequestOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use KademliaQueryRequest.newBuilder() to construct.
    private KademliaQueryRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private KademliaQueryRequest() {
      fromTime_ = 0L;
      toTime_ = 0L;
      idList_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private KademliaQueryRequest(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              fromTime_ = input.readInt64();
              break;
            }
            case 16: {

              toTime_ = input.readInt64();
              break;
            }
            case 26: {
              java.lang.String s = input.readStringRequireUtf8();
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                idList_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000004;
              }
              idList_.add(s);
              break;
            }
            default: {
              if (!parseUnknownFieldProto3(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          idList_ = idList_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.class, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.Builder.class);
    }

    private int bitField0_;
    public static final int FROMTIME_FIELD_NUMBER = 1;
    private long fromTime_;
    /**
     * <pre>
     * Times
     * </pre>
     *
     * <code>int64 fromTime = 1;</code>
     */
    public long getFromTime() {
      return fromTime_;
    }

    public static final int TOTIME_FIELD_NUMBER = 2;
    private long toTime_;
    /**
     * <code>int64 toTime = 2;</code>
     */
    public long getToTime() {
      return toTime_;
    }

    public static final int IDLIST_FIELD_NUMBER = 3;
    private com.google.protobuf.LazyStringList idList_;
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    public com.google.protobuf.ProtocolStringList
        getIdListList() {
      return idList_;
    }
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    public int getIdListCount() {
      return idList_.size();
    }
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    public java.lang.String getIdList(int index) {
      return idList_.get(index);
    }
    /**
     * <pre>
     * ID query
     * </pre>
     *
     * <code>repeated string idList = 3;</code>
     */
    public com.google.protobuf.ByteString
        getIdListBytes(int index) {
      return idList_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (fromTime_ != 0L) {
        output.writeInt64(1, fromTime_);
      }
      if (toTime_ != 0L) {
        output.writeInt64(2, toTime_);
      }
      for (int i = 0; i < idList_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, idList_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (fromTime_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(1, fromTime_);
      }
      if (toTime_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(2, toTime_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < idList_.size(); i++) {
          dataSize += computeStringSizeNoTag(idList_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getIdListList().size();
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest)) {
        return super.equals(obj);
      }
      io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest other = (io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest) obj;

      boolean result = true;
      result = result && (getFromTime()
          == other.getFromTime());
      result = result && (getToTime()
          == other.getToTime());
      result = result && getIdListList()
          .equals(other.getIdListList());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + FROMTIME_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getFromTime());
      hash = (37 * hash) + TOTIME_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getToTime());
      if (getIdListCount() > 0) {
        hash = (37 * hash) + IDLIST_FIELD_NUMBER;
        hash = (53 * hash) + getIdListList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest)
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.class, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.Builder.class);
      }

      // Construct using io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        fromTime_ = 0L;

        toTime_ = 0L;

        idList_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_descriptor;
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest getDefaultInstanceForType() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.getDefaultInstance();
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest build() {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest buildPartial() {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest result = new io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        result.fromTime_ = fromTime_;
        result.toTime_ = toTime_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          idList_ = idList_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.idList_ = idList_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return (Builder) super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return (Builder) super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest) {
          return mergeFrom((io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest other) {
        if (other == io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest.getDefaultInstance()) return this;
        if (other.getFromTime() != 0L) {
          setFromTime(other.getFromTime());
        }
        if (other.getToTime() != 0L) {
          setToTime(other.getToTime());
        }
        if (!other.idList_.isEmpty()) {
          if (idList_.isEmpty()) {
            idList_ = other.idList_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureIdListIsMutable();
            idList_.addAll(other.idList_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private long fromTime_ ;
      /**
       * <pre>
       * Times
       * </pre>
       *
       * <code>int64 fromTime = 1;</code>
       */
      public long getFromTime() {
        return fromTime_;
      }
      /**
       * <pre>
       * Times
       * </pre>
       *
       * <code>int64 fromTime = 1;</code>
       */
      public Builder setFromTime(long value) {
        
        fromTime_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Times
       * </pre>
       *
       * <code>int64 fromTime = 1;</code>
       */
      public Builder clearFromTime() {
        
        fromTime_ = 0L;
        onChanged();
        return this;
      }

      private long toTime_ ;
      /**
       * <code>int64 toTime = 2;</code>
       */
      public long getToTime() {
        return toTime_;
      }
      /**
       * <code>int64 toTime = 2;</code>
       */
      public Builder setToTime(long value) {
        
        toTime_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>int64 toTime = 2;</code>
       */
      public Builder clearToTime() {
        
        toTime_ = 0L;
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList idList_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      private void ensureIdListIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          idList_ = new com.google.protobuf.LazyStringArrayList(idList_);
          bitField0_ |= 0x00000004;
         }
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public com.google.protobuf.ProtocolStringList
          getIdListList() {
        return idList_.getUnmodifiableView();
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public int getIdListCount() {
        return idList_.size();
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public java.lang.String getIdList(int index) {
        return idList_.get(index);
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public com.google.protobuf.ByteString
          getIdListBytes(int index) {
        return idList_.getByteString(index);
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public Builder setIdList(
          int index, java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureIdListIsMutable();
        idList_.set(index, value);
        onChanged();
        return this;
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public Builder addIdList(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureIdListIsMutable();
        idList_.add(value);
        onChanged();
        return this;
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public Builder addAllIdList(
          java.lang.Iterable<java.lang.String> values) {
        ensureIdListIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, idList_);
        onChanged();
        return this;
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public Builder clearIdList() {
        idList_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }
      /**
       * <pre>
       * ID query
       * </pre>
       *
       * <code>repeated string idList = 3;</code>
       */
      public Builder addIdListBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        ensureIdListIsMutable();
        idList_.add(value);
        onChanged();
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFieldsProto3(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest)
    }

    // @@protoc_insertion_point(class_scope:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryRequest)
    private static final io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest();
    }

    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<KademliaQueryRequest>
        PARSER = new com.google.protobuf.AbstractParser<KademliaQueryRequest>() {
      @java.lang.Override
      public KademliaQueryRequest parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new KademliaQueryRequest(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<KademliaQueryRequest> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<KademliaQueryRequest> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryRequest.KademliaQueryRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\034BgKademliaQueryRequest.proto\022\'io.hbt.b" +
      "ubblegum.core.kademlia.protobuf\"H\n\024Kadem" +
      "liaQueryRequest\022\020\n\010fromTime\030\001 \001(\003\022\016\n\006toT" +
      "ime\030\002 \001(\003\022\016\n\006idList\030\003 \003(\tb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryRequest_descriptor,
        new java.lang.String[] { "FromTime", "ToTime", "IdList", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
