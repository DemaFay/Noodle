package com.noodle.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 *
 */

public class FileMappedBufferStorage extends ByteBufferStorage {

  private RandomAccessFile randomAccessFile;

  public FileMappedBufferStorage(final File file) throws IOException {
    randomAccessFile = new RandomAccessFile(file, "rw");

    final boolean existed = file.exists();
    if (!existed) {
      //noinspection ResultOfMethodCallIgnored
      file.createNewFile();
    }

    if (randomAccessFile.length() < INITIAL_SIZE) {
      randomAccessFile.setLength(INITIAL_SIZE);
    }

    buffer = randomAccessFile.getChannel()
        .map(FileChannel.MapMode.READ_WRITE, 0, randomAccessFile.length());

    if (existed) {
      remapIndexes();
    }
  }

  private void remapIndexes() {
    buffer.position(0);
    while (buffer.position() < buffer.limit()) {
      final int keySize = buffer.getInt();

      if (keySize == 0) {
        break;
      }

      final int dataSize = buffer.getInt();
      final byte[] key = new byte[keySize];
      buffer.get(key);
      buffer.position(buffer.position() + dataSize);

      treeMapIndex.put(new BytesWrapper(key), lastPosition);

      lastPosition = buffer.position();
    }
  }

  @Override
  protected void growBufferSize() {
    try {
      // Nice to have some reverse-exponential growth here.
      randomAccessFile.setLength(randomAccessFile.length() * 2);

      buffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, randomAccessFile.length());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}