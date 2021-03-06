import {Component, OnDestroy, OnInit} from '@angular/core';
import {VesselService} from "../../../vessels/services/vessel.service";
import {MapService} from "../../map/services/map.service";
import * as moment from "moment";
import {MonitoredVessel} from "../../../vessels/model/vessel.type";
import {MapState} from "../../map/type/map.type";
import {UserVesselService} from "../../../vessels/services/user-vessel.service";
import {catchError, EMPTY, Subscription} from "rxjs";
import {NgxSmartModalService} from "ngx-smart-modal";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-app-panel',
  templateUrl: './app-panel.component.html',
  styleUrls: ['./app-panel.component.scss']
})
export class AppPanelComponent implements OnInit, OnDestroy {

  vessels: MonitoredVessel[] = [];

  private monitoredVesselsSubscription: Subscription;
  private blockButtons = false;

  constructor(private mapService: MapService,
              private vesselService: VesselService,
              private userVesselService: UserVesselService,
              private ngxSmartModalService: NgxSmartModalService,
              private toastrService: ToastrService) {
    this.monitoredVesselsSubscription = this.userVesselService.userVessels$
      .subscribe(vessels => {
        this.vessels = vessels;
      });
  }

  ngOnInit() {
    this.mapService.changeState(MapState.AppMode);
  }

  ngOnDestroy() {
    this.monitoredVesselsSubscription.unsubscribe();
  }

  adjust(vessel: MonitoredVessel) {
    let latestPoint = vessel.tracks.flatMap(
      track => track.pointsInTime
    ).sort((point1, point2) => {
      return moment(point1.timestamp) < moment(point2.timestamp) ? 1 : -1
    })[0];

    this.mapService.centerOn(latestPoint.coordinates.latitude, latestPoint.coordinates.longitude, 10);
  }

  track(vessel: MonitoredVessel) {
    this.blockButtons = true;
    this.vesselService.trackVessel({ mmsi: vessel.mmsi, name: vessel.name, shipType: vessel.shipType })
      .pipe(
        catchError(err => {
          this.toastrService.error(err.error.message);
          return EMPTY;
        })
      )
      .subscribe(
      () => {
        this.mapService.changeState(MapState.RefreshUserVessels);
        this.blockButtons = false;
      });
  }

  suspend(mmsi: number) {
    this.blockButtons = true;
    this.vesselService.suspendTrackingVessel(mmsi).subscribe(
      () => {
        this.mapService.changeState(MapState.RefreshUserVessels);
        this.blockButtons = false;
      });
  }

  remove(mmsi: number) {
    this.blockButtons = true;
    this.vesselService.removeTrackedVessel(mmsi).subscribe(
      () => {
        this.mapService.changeState(MapState.RefreshUserVessels);
        this.blockButtons = false;
      });
  }

  openInfoModal(vessel: MonitoredVessel) {
    this.ngxSmartModalService.resetModalData('vesselInfoModal');
    this.ngxSmartModalService.setModalData(vessel, 'vesselInfoModal');
    this.ngxSmartModalService.getModal('vesselInfoModal').open();
  }
}
