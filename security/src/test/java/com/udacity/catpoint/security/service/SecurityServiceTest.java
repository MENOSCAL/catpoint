package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.MOTION);
    }

    //Test 1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void notAlarm_systemArmedAndSensorActive_pendingAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test 2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void pendingAlarm_systemArmedAndSensorActive_alarm(ArmingStatus armingStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 3
    @Test
    void pendingAlarmAndAllSensorInactive_notAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true); //changeSensorActivationStatus -> then sensors deactivated
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alarm_sensors_noChangeStatus(boolean bool) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, bool);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test 5
    @Test
    void sensorActive_pendingAlarmAndActivationTrue_alarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 6
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    void sensorInactive_alarmsAndActivationFalse_noChangeStatus(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test 7
    @Test
    void catImage_armedHome_alarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(eq(any(BufferedImage.class)));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 8
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    void notCatImageAndAllSensorInactive_alarms_notAlarm(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);

        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(eq(any(BufferedImage.class)));
        sensor.setActive(true); //changeSensorActivationStatus -> then sensors deactivated
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, atLeast(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 9
    @Test
    void systemDisarmed_notAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_resetSensorsFalse(ArmingStatus armingStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);

        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    //Test 11
    @Test
    void armedHome_CatImageCamera_Alarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(eq(any(BufferedImage.class)));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

}
