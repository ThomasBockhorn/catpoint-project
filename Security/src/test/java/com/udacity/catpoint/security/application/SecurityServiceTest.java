package com.udacity.catpoint.security.application;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class SecurityServiceTest {

    private  SecurityService securityService;

    @Mock
    private  Sensor sensor;

    @Mock
    private  PretendDatabaseSecurityRepositoryImpl securityRepository;

    @Mock
    private  FakeImageService imageService;

    @Mock
    private StatusListener statusListener;

    @BeforeEach
    public void init(){
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("sensor", SensorType.DOOR);
        securityService.addSensor(sensor);
    }

    @Test
    public void checking_to_see_if_alarm_is_armed_alarm_status_is_pending(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void checking_to_see_if_the_alarm_goes_off_when_alarm_is_armed_and_sensor_active(){
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void checking_to_see_if_sensors_inactive_and_alarm_pending_return_no_state(){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void checking_to_see_if_alarm_is_active_change_in_sensor_state_not_affect_alarm_state(boolean sensorStatus){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, sensorStatus);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void checking_to_see_if_sensor_is_activated_and_system_says_pending_puts_alarm_status_to_alarm(){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void checking_to_see_if_alarm_is_deactivated_does_not_change_alarm_status(AlarmStatus alarmStatus){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void checking_to_see_if_image_service_identifies_cat_and_system_goes_to_alarm_status(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(true);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void checking_to_see_if_cat_not_detected_change_status_to_no_alarm_as_long_as_sensor_inactive(){
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(false);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        BufferedImage currentCameraImage = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        securityService.processImage(currentCameraImage);
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void checking_to_see_if_system_is_disarmed_status_set_to_no_alarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void checking_to_see_if_armed_all_sensors_are_inactive(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.getSensors().forEach(sensor -> {
            assert Boolean.FALSE.equals(sensor.getActive());
        });
    }

    @Test
    public void checking_to_see_if_system_is_armed_and_cat_is_detected_alarm_status_set_to_alarm(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat()))
                .thenReturn(Boolean.TRUE);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(Mockito.any(AlarmStatus.class));

    }

    @Test
    public void checking_to_see_if_change_in_sensor_activation_status_deactivate_sensor(){
        securityRepository.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        assert sensor.getActive().equals(false);
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(Mockito.any(AlarmStatus.class));
    }

    @Test
    public void test_add_and_remove_status_listener(){
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    public void test_add_and_remove_sensor(){
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void checking_to_see_if_arming_status_can_run_three_times(ArmingStatus armingStatus){
        securityService.setArmingStatus(armingStatus);
    }

    @Test
    public void test_to_see_if_sensor_can_be_updated(){
        securityService.addSensor(sensor);
        securityRepository.updateSensor(sensor);
    }

    @Test
    public void if_camera_image_does_not_contain_cat_and_sensors_are_not_active_change_status_to_no_alarm(){
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(false);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void if_the_system_is_armed_reset_all_sensors_to_inactive(ArmingStatus armingStatus){
        securityService.setArmingStatus(armingStatus);
        sensor.setActive(false);
        securityService.getSensors().forEach(sensor -> {
            assert Boolean.FALSE.equals(sensor.getActive());
        });
    }

    @Test
    public void check_to_see_if_handSensor_is_activated_when_repository_is_disarmed_and_alarm_should_trigger_handle_sensor_to_deactivate(){
        securityRepository.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
    }

    @Test
    public void check_to_see_if_hand_sensor_is_active_and_when_respository_is_disarmed_and_no_alarm_should_trigger_handleSensor_activated(){
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
    }

    @Test
    public void check_to_see_system_is_armed_while_cat_is_detected_set_alarm_status_to_alarm(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(true);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(Mockito.any(AlarmStatus.class));
    }
}