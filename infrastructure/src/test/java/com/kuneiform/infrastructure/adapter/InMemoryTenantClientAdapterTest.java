package com.kuneiform.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties.ClientConfig; // Assuming this inner class exists
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryTenantClientAdapterTest {

  @Mock private WedgeConfigProperties wedgeConfig;

  private InMemoryTenantClientAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InMemoryTenantClientAdapter(wedgeConfig);
  }

  @Test
  void shouldInitializeFromConfig() {
    // Setup config mocks
    ClientConfig client1 = mock(ClientConfig.class);
    when(client1.getClientId()).thenReturn("client-1");
    when(client1.getTenantId()).thenReturn("tenant-A");

    ClientConfig client2 = mock(ClientConfig.class);
    when(client2.getClientId()).thenReturn("client-2");
    when(client2.getTenantId()).thenReturn("tenant-A");

    ClientConfig client3 = mock(ClientConfig.class);
    when(client3.getClientId()).thenReturn("client-3");
    when(client3.getTenantId()).thenReturn("tenant-B");

    when(wedgeConfig.getClients()).thenReturn(List.of(client1, client2, client3));

    // Execute init
    adapter.init();

    // Verify
    assertThat(adapter.getClientIdsForTenant("tenant-A"))
        .containsExactlyInAnyOrder("client-1", "client-2");
    assertThat(adapter.getClientIdsForTenant("tenant-B")).containsExactly("client-3");
    assertThat(adapter.isClientInTenant("tenant-A", "client-1")).isTrue();
    assertThat(adapter.isClientInTenant("tenant-B", "client-1")).isFalse();
  }

  @Test
  void shouldAddAndRemoveClientsDynamicall() {
    // Empty config start
    when(wedgeConfig.getClients()).thenReturn(List.of());
    adapter.init();

    String tenantId = "dynamic-tenant";
    String clientId = "dynamic-client";

    adapter.addClientToTenant(tenantId, clientId);
    assertThat(adapter.isClientInTenant(tenantId, clientId)).isTrue();
    assertThat(adapter.getClientIdsForTenant(tenantId)).containsExactly(clientId);

    adapter.removeClientFromTenant(tenantId, clientId);
    assertThat(adapter.isClientInTenant(tenantId, clientId)).isFalse();
    assertThat(adapter.getClientIdsForTenant(tenantId)).isEmpty();
  }
}
