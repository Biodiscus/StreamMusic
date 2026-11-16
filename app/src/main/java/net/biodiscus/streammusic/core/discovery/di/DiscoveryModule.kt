package net.biodiscus.streammusic.core.discovery.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import net.biodiscus.streammusic.core.discovery.DeviceDiscovery
import net.biodiscus.streammusic.core.discovery.DeviceRepository
import net.biodiscus.streammusic.core.discovery.NetworkDeviceRepository
import net.biodiscus.streammusic.core.discovery.mdns.MdnsDeviceDiscovery
import net.biodiscus.streammusic.core.discovery.ssdp.SsdpDeviceDiscovery

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DiscoveryModule {

    @Binds
    abstract fun bindDeviceRepository(repository: NetworkDeviceRepository): DeviceRepository

    @Binds
    @IntoSet
    abstract fun bindSsdpDiscovery(discovery: SsdpDeviceDiscovery): DeviceDiscovery

    @Binds
    @IntoSet
    abstract fun bindMdnsDiscovery(discovery: MdnsDeviceDiscovery): DeviceDiscovery
}
