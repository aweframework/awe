package com.almis.awe.observability.instrumentation;

import com.almis.awe.service.observation.FileTransferObserver.Direction;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MicrometerFileTransferObserverTest {

  @Test
  @DisplayName("Uploads and downloads are counted and their bytes summed on awe.files, by direction")
  void countsFilesAndBytesByDirection() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerFileTransferObserver observer = new MicrometerFileTransferObserver(() -> registry);

    observer.transferred(Direction.UPLOAD, 100);
    observer.transferred(Direction.UPLOAD, 50);
    observer.transferred(Direction.DOWNLOAD, 2048);

    DistributionSummary uploads = registry.get(MicrometerFileTransferObserver.METER).tags("direction", "upload").summary();
    DistributionSummary downloads = registry.get(MicrometerFileTransferObserver.METER).tags("direction", "download").summary();
    assertThat(uploads.count()).isEqualTo(2);
    assertThat(uploads.totalAmount()).isEqualTo(150.0);
    assertThat(downloads.count()).isEqualTo(1);
    assertThat(downloads.totalAmount()).isEqualTo(2048.0);
    assertThat(uploads.getId().getBaseUnit()).isEqualTo("bytes");
    assertThat(registry.find(MicrometerFileTransferObserver.METER).meters()).hasSize(2);
  }

  @Test
  @DisplayName("Without a meter registry nothing is recorded and nothing fails")
  void doesNothingWithoutRegistry() {
    MicrometerFileTransferObserver observer = new MicrometerFileTransferObserver(() -> null);

    assertThatCode(() -> observer.transferred(Direction.DOWNLOAD, 10)).doesNotThrowAnyException();
  }
}
