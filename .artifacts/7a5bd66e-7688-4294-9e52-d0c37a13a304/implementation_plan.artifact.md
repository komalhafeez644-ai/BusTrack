# Implementation Plan - Dynamic Live Tracking (ETA, Speed, Load)

This plan outlines the steps to replace hardcoded values in the Admin's Live Tracking screen with real-time data for ETA, Speed, and Load.

## Proposed Changes

### [Backend/Data Layer]

#### [MODIFY] [DriverModel.kt](file:///C:/Users/dell/Desktop/BusTrack/app/src/main/java/com/example/bustrack_app/models/DriverModel.kt)
- Add `speed` field to the data class to store calculated speed.

### [ViewModel Layer]

#### [MODIFY] [LiveTrackingViewModel.kt](file:///C:/Users/dell/Desktop/BusTrack/app/src/main/java/com/example/bustrack_app/viewmodels/LiveTrackingViewModel.kt)
- Implement Speed Calculation:
    - Use `previousLocations` map to store the last known position and timestamp for each driver.
    - Calculate speed = (distance / time) whenever location updates.
- Implement ETA Calculation:
    - Fetch the `RouteModel` associated with the driver's assigned route.
    - Identify the "Next Stop" based on proximity and route sequence.
    - Calculate ETA = (distance to next stop / speed).
- Implement Load Calculation:
    - Fetch students assigned to the route.
    - Fetch today's attendance records.
    - **Morning Logic**: `LOAD = (Count of Students with morningPickup == "Present") / (Total Students on Route)`.
    - **Evening Logic**: `LOAD = (Count of Students with eveningPickup == "Present" - Count of Students with eveningDrop == "Dropped") / (Total Students with eveningPickup == "Present")`.
- Expose these calculated values to the View.

### [UI Layer]

#### [MODIFY] [LiveTrackingActivity.kt](file:///C:/Users/dell/Desktop/BusTrack/app/src/main/java/ui/admin/LiveTrackingActivity.kt)
- Update `updateDriverCard` to use the dynamic values from the ViewModel instead of hardcoded strings.
- Format ETA display (e.g., "4 min", "Arriving").
- Format Load display (e.g., "23/45").

## Verification Plan

### Automated Tests
- N/A (Manual verification on device/emulator is preferred for real-time location features).

### Manual Verification
- Deploy the app and simulate location updates for a driver.
- Verify that the Speed updates on the Admin card.
- Verify that ETA changes as the bus moves closer to a stop.
- Mark students as "Present" in the Driver module and verify the Load count increases/decreases correctly in the Admin module.
