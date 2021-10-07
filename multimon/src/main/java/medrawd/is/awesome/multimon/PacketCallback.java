package medrawd.is.awesome.multimon;

public interface PacketCallback {
    void received(byte[] packet);
    void received(char sign);
}