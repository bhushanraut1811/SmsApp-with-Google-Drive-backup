package apps.sample.smsapp.model;

/**
 * Interface which contains methods for Conversation Screen
 */
public interface IConversation {

    void createNewSMS(String sms, String number);

    void sendSMS(String sms);

    // void deleteSMS();
}
