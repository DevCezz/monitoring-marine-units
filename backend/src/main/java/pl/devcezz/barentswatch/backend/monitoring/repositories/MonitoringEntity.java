package pl.devcezz.barentswatch.backend.monitoring.repositories;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import pl.devcezz.barentswatch.backend.common.CurrentVesselRegistry;
import pl.devcezz.barentswatch.backend.common.UserMonitoring;
import pl.devcezz.barentswatch.backend.common.VesselToTrack;
import pl.devcezz.barentswatch.backend.monitoring.exceptions.VesselAlreadySuspendedException;
import pl.devcezz.barentswatch.backend.monitoring.exceptions.VesselIsNotTrackedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@MongoEntity(collection = "monitoring")
public class MonitoringEntity {

    public ObjectId id;
    public String email;
    public List<VesselEntity> trackedVessels = new ArrayList<>();

    public static MonitoringEntity createUser(String email) {
        MonitoringEntity monitoringEntity = new MonitoringEntity();
        monitoringEntity.email = email;
        return monitoringEntity;
    }

    public void trackVessel(VesselToTrack vesselToTrack) {
        Optional<VesselEntity> containedVessel = trackedVessels.stream()
                .filter(vessel -> vessel.isFor(vesselToTrack.mmsi()))
                .findFirst();

        containedVessel.ifPresentOrElse(
                VesselEntity::resumeTracking,
                () -> trackedVessels.add(VesselEntity.createNewVessel(vesselToTrack))
        );
    }

    public List<UserMonitoring> convertToMonitoringList() {
        return trackedVessels.stream()
                .map(VesselEntity::convertToUserMonitoring)
                .toList();
    }

    public void suspendTrackingVessel(Integer mmsi) {
        if (trackedVessels.stream().anyMatch(vessel -> vessel.isSuspended(mmsi))) {
            throw new VesselAlreadySuspendedException();
        }

        trackedVessels.stream()
                .filter(vessel -> vessel.isTracking(mmsi))
                .findFirst()
                .ifPresent(VesselEntity::suspendTracking);
    }

    public void removeTrackingVessel(Integer mmsi) {
        if (trackedVessels.stream().noneMatch(vessel -> vessel.isFor(mmsi))) {
            throw new VesselIsNotTrackedException();
        }
        trackedVessels.removeIf(vessel -> vessel.isFor(mmsi));
    }

    public void addPointForVessel(CurrentVesselRegistry currentVesselRegistry) {
        if (trackedVessels.stream().noneMatch(vessel -> vessel.isTracking(currentVesselRegistry.mmsi()))) {
            return;
        }

        trackedVessels.stream()
                .filter(vessel -> vessel.isTracking(currentVesselRegistry.mmsi()))
                .findFirst()
                .ifPresent(vessel -> vessel.addPoint(currentVesselRegistry));
    }

    public List<Integer> trackedMmsi() {
        return trackedVessels.stream()
                .map(vessel -> vessel.mmsi)
                .toList();
    }

    public Long numberOfTrackedVessels() {
        return trackedVessels.stream()
                .filter(trackedVessel -> trackedVessel.status.equals(VesselEntity.Status.TRACKED))
                .count();
    }

    public boolean isTracking(Integer mmsi) {
        return trackedVessels.stream()
                .anyMatch(trackedVessel -> Objects.equals(trackedVessel.mmsi, mmsi));
    }
}

