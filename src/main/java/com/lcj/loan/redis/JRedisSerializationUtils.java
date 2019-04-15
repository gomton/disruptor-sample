package com.lcj.loan.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import redis.clients.jedis.exceptions.JedisException;

public class JRedisSerializationUtils {
	 public JRedisSerializationUtils() {  }


	    // Serialize
	    //-----------------------------------------------------------------------

	    //  In order to optimize object reuse and thread safety,
	    // FSTConfiguration provides 2 simple factory methods to
	    // obtain input/outputstream instances (they are stored thread local):
	    //! reuse this Object, it caches metadata. Performance degrades massively
	    //using createDefaultConfiguration()        FSTConfiguration is singleton

	    /**
	     * <p>Serializes an <code>Object</code> to a byte array for
	     * storage/serialization.</p>
	     *
	     * @param obj the object to serialize to bytes
	     * @return a byte[] with the converted Serializable
	     * @throws JRedisCacheException (runtime) if the serialization fails
	     */
	    public static byte[] fastSerialize(Object obj) {
	        ByteArrayOutputStream byteArrayOutputStream = null;
	        FSTObjectOutput out = null;
	        try {
	            // stream closed in the finally
	            byteArrayOutputStream = new ByteArrayOutputStream(512);
	            out = new FSTObjectOutput(byteArrayOutputStream);  //32000  buffer size
	            out.writeObject(obj);
	            out.flush();
	            return byteArrayOutputStream.toByteArray();
	        } catch (IOException ex) {
	            throw new JedisException(ex);
	        } finally {
	            try {
	                obj = null;
	                if (out != null) {
	                    out.close();    //call flush byte buffer
	                    out = null;
	                }
	                if (byteArrayOutputStream != null) {

	                    byteArrayOutputStream.close();
	                    byteArrayOutputStream = null;
	                }
	            } catch (IOException ex) {
	                // ignore close exception
	            }
	        }
	    }
	    // Deserialize
	    //-----------------------------------------------------------------------

	    /**
	     * <p>Deserializes a single <code>Object</code> from an array of bytes.</p>
	     *
	     * @param objectData the serialized object, must not be null
	     * @return the deserialized object
	     * @throws IllegalArgumentException if <code>objectData</code> is <code>null</code>
	     * @throws JRedisCacheException     (runtime) if the serialization fails
	     */
	    public static Object fastDeserialize(byte[] objectData) throws Exception {
	        ByteArrayInputStream byteArrayInputStream = null;
	        FSTObjectInput in = null;
	        try {
	            // stream closed in the finally
	            byteArrayInputStream = new ByteArrayInputStream(objectData);
	            in = new FSTObjectInput(byteArrayInputStream);
	            return in.readObject();
	        } catch (ClassNotFoundException ex) {
	            throw new JedisException(ex);
	        } catch (IOException ex) {
	            throw new JedisException(ex);
	        } finally {
	            try {
	                objectData = null;
	                if (in != null) {
	                    in.close();
	                    in = null;
	                }
	                if (byteArrayInputStream != null) {
	                    byteArrayInputStream.close();
	                    byteArrayInputStream = null;
	                }
	            } catch (IOException ex) {
	                // ignore close exception
	            }
	        }
	    }
 

	    //jdk原生序列换方案

	    /**
	     * @param obj
	     * @return
	     */
	    public static byte[] jserialize(Object obj) {
	        ObjectOutputStream oos = null;
	        ByteArrayOutputStream baos = null;
	        try {
	            baos = new ByteArrayOutputStream();
	            oos = new ObjectOutputStream(baos);
	            oos.writeObject(obj);
	            return baos.toByteArray();
	        } catch (IOException e) {
	            throw new JedisException(e);
	        } finally {
	            if (oos != null)
	                try {
	                    oos.close();
	                    baos.close();
	                } catch (IOException e) {
	                }
	        }
	    }

	    /**
	     * @param bits
	     * @return
	     */
	    public static Object jdeserialize(byte[] bits) {
	        ObjectInputStream ois = null;
	        ByteArrayInputStream bais = null;
	        try {
	            bais = new ByteArrayInputStream(bits);
	            ois = new ObjectInputStream(bais);
	            return ois.readObject();
	        } catch (Exception e) {
	            throw new JedisException(e);
	        } finally {
	            if (ois != null)
	                try {
	                    ois.close();
	                    bais.close();
	                } catch (IOException e) {
	                }
	        }
	    }


	    // 基于protobuffer的序列化方案

	    /**
	     * @param bytes       字节数据
	     * @param messageLite 序列化对应的类型
	     * @return
	     * @throws JRedisCacheException
	     */
	    public static MessageLite protoDeserialize(byte[] bytes, MessageLite messageLite) throws JedisException {
	        assert (bytes != null && messageLite != null);
	        try {
	            return messageLite.getParserForType().parsePartialFrom(CodedInputStream.newInstance(bytes), ExtensionRegistryLite.getEmptyRegistry());
	        } catch (InvalidProtocolBufferException e) {
	            e.printStackTrace();
	            return null;
	        }
	    }

	    /**
	     * @param messageLite 序列化对应的类型
	     * @return
	     * @throws JRedisCacheException
	     */
	    public static byte[] protoSerialize(MessageLite messageLite) throws JedisException {
	        assert (messageLite != null);
	        return messageLite.toByteArray();
	    }
}
