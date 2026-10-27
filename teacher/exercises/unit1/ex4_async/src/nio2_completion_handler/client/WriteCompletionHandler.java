package nio2_completion_handler.client;



import common.Demo;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

class WriteCompletionHandler extends Demo implements CompletionHandler<Integer, Attachment> {

    private final AsynchronousSocketChannel socketChannel;

    WriteCompletionHandler(AsynchronousSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void completed(Integer bytesWritten, Attachment attachment) {
        logger.info("Echo client wrote: {} byte(s)"+ bytesWritten);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler(buffer);
        socketChannel.read(buffer, attachment, readCompletionHandler);
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        logger.severe("Exception during socket writing"+ t);
    }
}