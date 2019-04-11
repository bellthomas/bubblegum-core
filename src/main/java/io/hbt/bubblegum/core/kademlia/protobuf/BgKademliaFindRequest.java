// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: BgKademliaFindRequest.proto

package io.hbt.bubblegum.core.kademlia.protobuf;

public final class BgKademliaFindRequest {
  private BgKademliaFindRequest() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface KademliaFindRequestOrBuilder extends
      // @@protoc_insertion_point(interface_extends:io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string searchHash = 1;</code>
     */
    java.lang.String getSearchHash();
    /**
     * <code>string searchHash = 1;</code>
     */
    com.google.protobuf.ByteString
        getSearchHashBytes();

    /**
     * <code>int32 numberRequested = 2;</code>
     */
    int getNumberRequested();

    /**
     * <code>bool returnValue = 3;</code>
     */
    boolean getReturnValue();
  }
  /**
   * Protobuf type {@code io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest}
   */
  public  static final class KademliaFindRequest extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest)
      KademliaFindRequestOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use KademliaFindRequest.newBuilder() to construct.
    private KademliaFindRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private KademliaFindRequest() {
      searchHash_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private KademliaFindRequest(
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
            case 10: {
              java.lang.String s = input.readStringRequireUtf8();

              searchHash_ = s;
              break;
            }
            case 16: {

              numberRequested_ = input.readInt32();
              break;
            }
            case 24: {

              returnValue_ = input.readBool();
              break;
            }
            default: {
              if (!parseUnknownField(
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
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.class, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.Builder.class);
    }

    public static final int SEARCHHASH_FIELD_NUMBER = 1;
    private volatile java.lang.Object searchHash_;
    /**
     * <code>string searchHash = 1;</code>
     */
    public java.lang.String getSearchHash() {
      java.lang.Object ref = searchHash_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        searchHash_ = s;
        return s;
      }
    }
    /**
     * <code>string searchHash = 1;</code>
     */
    public com.google.protobuf.ByteString
        getSearchHashBytes() {
      java.lang.Object ref = searchHash_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        searchHash_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int NUMBERREQUESTED_FIELD_NUMBER = 2;
    private int numberRequested_;
    /**
     * <code>int32 numberRequested = 2;</code>
     */
    public int getNumberRequested() {
      return numberRequested_;
    }

    public static final int RETURNVALUE_FIELD_NUMBER = 3;
    private boolean returnValue_;
    /**
     * <code>bool returnValue = 3;</code>
     */
    public boolean getReturnValue() {
      return returnValue_;
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
      if (!getSearchHashBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, searchHash_);
      }
      if (numberRequested_ != 0) {
        output.writeInt32(2, numberRequested_);
      }
      if (returnValue_ != false) {
        output.writeBool(3, returnValue_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!getSearchHashBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, searchHash_);
      }
      if (numberRequested_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(2, numberRequested_);
      }
      if (returnValue_ != false) {
        size += com.google.protobuf.CodedOutputStream
          .computeBoolSize(3, returnValue_);
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
      if (!(obj instanceof io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest)) {
        return super.equals(obj);
      }
      io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest other = (io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest) obj;

      if (!getSearchHash()
          .equals(other.getSearchHash())) return false;
      if (getNumberRequested()
          != other.getNumberRequested()) return false;
      if (getReturnValue()
          != other.getReturnValue()) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + SEARCHHASH_FIELD_NUMBER;
      hash = (53 * hash) + getSearchHash().hashCode();
      hash = (37 * hash) + NUMBERREQUESTED_FIELD_NUMBER;
      hash = (53 * hash) + getNumberRequested();
      hash = (37 * hash) + RETURNVALUE_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
          getReturnValue());
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parseFrom(
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
    public static Builder newBuilder(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest prototype) {
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
     * Protobuf type {@code io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest)
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.class, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.Builder.class);
      }

      // Construct using io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.newBuilder()
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
        searchHash_ = "";

        numberRequested_ = 0;

        returnValue_ = false;

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_descriptor;
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest getDefaultInstanceForType() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.getDefaultInstance();
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest build() {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest buildPartial() {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest result = new io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest(this);
        result.searchHash_ = searchHash_;
        result.numberRequested_ = numberRequested_;
        result.returnValue_ = returnValue_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest) {
          return mergeFrom((io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest other) {
        if (other == io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest.getDefaultInstance()) return this;
        if (!other.getSearchHash().isEmpty()) {
          searchHash_ = other.searchHash_;
          onChanged();
        }
        if (other.getNumberRequested() != 0) {
          setNumberRequested(other.getNumberRequested());
        }
        if (other.getReturnValue() != false) {
          setReturnValue(other.getReturnValue());
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
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private java.lang.Object searchHash_ = "";
      /**
       * <code>string searchHash = 1;</code>
       */
      public java.lang.String getSearchHash() {
        java.lang.Object ref = searchHash_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          searchHash_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string searchHash = 1;</code>
       */
      public com.google.protobuf.ByteString
          getSearchHashBytes() {
        java.lang.Object ref = searchHash_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          searchHash_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string searchHash = 1;</code>
       */
      public Builder setSearchHash(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        searchHash_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>string searchHash = 1;</code>
       */
      public Builder clearSearchHash() {
        
        searchHash_ = getDefaultInstance().getSearchHash();
        onChanged();
        return this;
      }
      /**
       * <code>string searchHash = 1;</code>
       */
      public Builder setSearchHashBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        searchHash_ = value;
        onChanged();
        return this;
      }

      private int numberRequested_ ;
      /**
       * <code>int32 numberRequested = 2;</code>
       */
      public int getNumberRequested() {
        return numberRequested_;
      }
      /**
       * <code>int32 numberRequested = 2;</code>
       */
      public Builder setNumberRequested(int value) {
        
        numberRequested_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>int32 numberRequested = 2;</code>
       */
      public Builder clearNumberRequested() {
        
        numberRequested_ = 0;
        onChanged();
        return this;
      }

      private boolean returnValue_ ;
      /**
       * <code>bool returnValue = 3;</code>
       */
      public boolean getReturnValue() {
        return returnValue_;
      }
      /**
       * <code>bool returnValue = 3;</code>
       */
      public Builder setReturnValue(boolean value) {
        
        returnValue_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>bool returnValue = 3;</code>
       */
      public Builder clearReturnValue() {
        
        returnValue_ = false;
        onChanged();
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest)
    }

    // @@protoc_insertion_point(class_scope:io.hbt.bubblegum.core.kademlia.protobuf.KademliaFindRequest)
    private static final io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest();
    }

    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<KademliaFindRequest>
        PARSER = new com.google.protobuf.AbstractParser<KademliaFindRequest>() {
      @java.lang.Override
      public KademliaFindRequest parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new KademliaFindRequest(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<KademliaFindRequest> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<KademliaFindRequest> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaFindRequest.KademliaFindRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\033BgKademliaFindRequest.proto\022\'io.hbt.bu" +
      "bblegum.core.kademlia.protobuf\"W\n\023Kademl" +
      "iaFindRequest\022\022\n\nsearchHash\030\001 \001(\t\022\027\n\017num" +
      "berRequested\030\002 \001(\005\022\023\n\013returnValue\030\003 \001(\010b" +
      "\006proto3"
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
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaFindRequest_descriptor,
        new java.lang.String[] { "SearchHash", "NumberRequested", "ReturnValue", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
