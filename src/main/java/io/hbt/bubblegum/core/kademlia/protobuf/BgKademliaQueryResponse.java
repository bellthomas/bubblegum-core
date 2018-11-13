// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: BgKademliaQueryResponse.proto

package io.hbt.bubblegum.core.kademlia.protobuf;

public final class BgKademliaQueryResponse {
  private BgKademliaQueryResponse() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface KademliaQueryResponseOrBuilder extends
      // @@protoc_insertion_point(interface_extends:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    java.util.List<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem> 
        getItemsList();
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem getItems(int index);
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    int getItemsCount();
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    java.util.List<? extends io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder> 
        getItemsOrBuilderList();
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder getItemsOrBuilder(
        int index);
  }
  /**
   * Protobuf type {@code io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse}
   */
  public  static final class KademliaQueryResponse extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse)
      KademliaQueryResponseOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use KademliaQueryResponse.newBuilder() to construct.
    private KademliaQueryResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private KademliaQueryResponse() {
      items_ = java.util.Collections.emptyList();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private KademliaQueryResponse(
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
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                items_ = new java.util.ArrayList<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem>();
                mutable_bitField0_ |= 0x00000001;
              }
              items_.add(
                  input.readMessage(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.parser(), extensionRegistry));
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
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          items_ = java.util.Collections.unmodifiableList(items_);
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.class, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.Builder.class);
    }

    public static final int ITEMS_FIELD_NUMBER = 1;
    private java.util.List<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem> items_;
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    public java.util.List<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem> getItemsList() {
      return items_;
    }
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    public java.util.List<? extends io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder> 
        getItemsOrBuilderList() {
      return items_;
    }
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    public int getItemsCount() {
      return items_.size();
    }
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem getItems(int index) {
      return items_.get(index);
    }
    /**
     * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
     */
    public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder getItemsOrBuilder(
        int index) {
      return items_.get(index);
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
      for (int i = 0; i < items_.size(); i++) {
        output.writeMessage(1, items_.get(i));
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      for (int i = 0; i < items_.size(); i++) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(1, items_.get(i));
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
      if (!(obj instanceof io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse)) {
        return super.equals(obj);
      }
      io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse other = (io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse) obj;

      boolean result = true;
      result = result && getItemsList()
          .equals(other.getItemsList());
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
      if (getItemsCount() > 0) {
        hash = (37 * hash) + ITEMS_FIELD_NUMBER;
        hash = (53 * hash) + getItemsList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parseFrom(
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
    public static Builder newBuilder(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse prototype) {
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
     * Protobuf type {@code io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse)
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.class, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.Builder.class);
      }

      // Construct using io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.newBuilder()
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
          getItemsFieldBuilder();
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        if (itemsBuilder_ == null) {
          items_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          itemsBuilder_.clear();
        }
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_descriptor;
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse getDefaultInstanceForType() {
        return io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.getDefaultInstance();
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse build() {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse buildPartial() {
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse result = new io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse(this);
        int from_bitField0_ = bitField0_;
        if (itemsBuilder_ == null) {
          if (((bitField0_ & 0x00000001) == 0x00000001)) {
            items_ = java.util.Collections.unmodifiableList(items_);
            bitField0_ = (bitField0_ & ~0x00000001);
          }
          result.items_ = items_;
        } else {
          result.items_ = itemsBuilder_.build();
        }
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
        if (other instanceof io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse) {
          return mergeFrom((io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse other) {
        if (other == io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse.getDefaultInstance()) return this;
        if (itemsBuilder_ == null) {
          if (!other.items_.isEmpty()) {
            if (items_.isEmpty()) {
              items_ = other.items_;
              bitField0_ = (bitField0_ & ~0x00000001);
            } else {
              ensureItemsIsMutable();
              items_.addAll(other.items_);
            }
            onChanged();
          }
        } else {
          if (!other.items_.isEmpty()) {
            if (itemsBuilder_.isEmpty()) {
              itemsBuilder_.dispose();
              itemsBuilder_ = null;
              items_ = other.items_;
              bitField0_ = (bitField0_ & ~0x00000001);
              itemsBuilder_ = 
                com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ?
                   getItemsFieldBuilder() : null;
            } else {
              itemsBuilder_.addAllMessages(other.items_);
            }
          }
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
        io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private java.util.List<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem> items_ =
        java.util.Collections.emptyList();
      private void ensureItemsIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          items_ = new java.util.ArrayList<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem>(items_);
          bitField0_ |= 0x00000001;
         }
      }

      private com.google.protobuf.RepeatedFieldBuilderV3<
          io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder> itemsBuilder_;

      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public java.util.List<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem> getItemsList() {
        if (itemsBuilder_ == null) {
          return java.util.Collections.unmodifiableList(items_);
        } else {
          return itemsBuilder_.getMessageList();
        }
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public int getItemsCount() {
        if (itemsBuilder_ == null) {
          return items_.size();
        } else {
          return itemsBuilder_.getCount();
        }
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem getItems(int index) {
        if (itemsBuilder_ == null) {
          return items_.get(index);
        } else {
          return itemsBuilder_.getMessage(index);
        }
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder setItems(
          int index, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem value) {
        if (itemsBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureItemsIsMutable();
          items_.set(index, value);
          onChanged();
        } else {
          itemsBuilder_.setMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder setItems(
          int index, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder builderForValue) {
        if (itemsBuilder_ == null) {
          ensureItemsIsMutable();
          items_.set(index, builderForValue.build());
          onChanged();
        } else {
          itemsBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder addItems(io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem value) {
        if (itemsBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureItemsIsMutable();
          items_.add(value);
          onChanged();
        } else {
          itemsBuilder_.addMessage(value);
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder addItems(
          int index, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem value) {
        if (itemsBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureItemsIsMutable();
          items_.add(index, value);
          onChanged();
        } else {
          itemsBuilder_.addMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder addItems(
          io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder builderForValue) {
        if (itemsBuilder_ == null) {
          ensureItemsIsMutable();
          items_.add(builderForValue.build());
          onChanged();
        } else {
          itemsBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder addItems(
          int index, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder builderForValue) {
        if (itemsBuilder_ == null) {
          ensureItemsIsMutable();
          items_.add(index, builderForValue.build());
          onChanged();
        } else {
          itemsBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder addAllItems(
          java.lang.Iterable<? extends io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem> values) {
        if (itemsBuilder_ == null) {
          ensureItemsIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(
              values, items_);
          onChanged();
        } else {
          itemsBuilder_.addAllMessages(values);
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder clearItems() {
        if (itemsBuilder_ == null) {
          items_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
          onChanged();
        } else {
          itemsBuilder_.clear();
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public Builder removeItems(int index) {
        if (itemsBuilder_ == null) {
          ensureItemsIsMutable();
          items_.remove(index);
          onChanged();
        } else {
          itemsBuilder_.remove(index);
        }
        return this;
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder getItemsBuilder(
          int index) {
        return getItemsFieldBuilder().getBuilder(index);
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder getItemsOrBuilder(
          int index) {
        if (itemsBuilder_ == null) {
          return items_.get(index);  } else {
          return itemsBuilder_.getMessageOrBuilder(index);
        }
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public java.util.List<? extends io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder> 
           getItemsOrBuilderList() {
        if (itemsBuilder_ != null) {
          return itemsBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(items_);
        }
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder addItemsBuilder() {
        return getItemsFieldBuilder().addBuilder(
            io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.getDefaultInstance());
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder addItemsBuilder(
          int index) {
        return getItemsFieldBuilder().addBuilder(
            index, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.getDefaultInstance());
      }
      /**
       * <code>repeated .io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponseItem items = 1;</code>
       */
      public java.util.List<io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder> 
           getItemsBuilderList() {
        return getItemsFieldBuilder().getBuilderList();
      }
      private com.google.protobuf.RepeatedFieldBuilderV3<
          io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder> 
          getItemsFieldBuilder() {
        if (itemsBuilder_ == null) {
          itemsBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<
              io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItem.Builder, io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.KademliaQueryResponseItemOrBuilder>(
                  items_,
                  ((bitField0_ & 0x00000001) == 0x00000001),
                  getParentForChildren(),
                  isClean());
          items_ = null;
        }
        return itemsBuilder_;
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


      // @@protoc_insertion_point(builder_scope:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse)
    }

    // @@protoc_insertion_point(class_scope:io.hbt.bubblegum.core.kademlia.protobuf.KademliaQueryResponse)
    private static final io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse();
    }

    public static io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<KademliaQueryResponse>
        PARSER = new com.google.protobuf.AbstractParser<KademliaQueryResponse>() {
      @java.lang.Override
      public KademliaQueryResponse parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new KademliaQueryResponse(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<KademliaQueryResponse> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<KademliaQueryResponse> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponse.KademliaQueryResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\035BgKademliaQueryResponse.proto\022\'io.hbt." +
      "bubblegum.core.kademlia.protobuf\032!BgKade" +
      "mliaQueryResponseItem.proto\"j\n\025KademliaQ" +
      "ueryResponse\022Q\n\005items\030\001 \003(\0132B.io.hbt.bub" +
      "blegum.core.kademlia.protobuf.KademliaQu" +
      "eryResponseItemb\006proto3"
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
          io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.getDescriptor(),
        }, assigner);
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_io_hbt_bubblegum_core_kademlia_protobuf_KademliaQueryResponse_descriptor,
        new java.lang.String[] { "Items", });
    io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaQueryResponseItem.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
