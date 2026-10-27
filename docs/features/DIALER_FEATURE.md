# Internal dialer — version 0.5 prototype

Home → «تماس» now opens Rosheno's own dial pad. Users can enter a number with large keys, paste/edit a number, delete one digit or clear it, and select a family number. The number display and call action stay outside the scrollable keypad. The typed number survives activity recreation and navigating between launcher pages.

«تماس با این شماره» requests `CALL_PHONE` only if needed, then presents a separate confirmation of the normalized number and matching family name. Granting permission never starts a call. The final «شروع تماس» dispatches an `ACTION_CALL` intent after checking permission again. Cancel does not dispatch any phone intent. Service/USSD codes and URI commands are rejected.

Known Iranian/global emergency numbers and platform-recognized emergency numbers use `ACTION_DIAL`, so the system phone application handles the emergency path. If call permission is declined, the number is retained and the user can explicitly continue with the system dialer. See [Android ACTION_CALL reference](https://developer.android.com/reference/android/content/Intent#ACTION_CALL).

The existing phone application manages the connected-call screen, incoming calls, audio controls and hangup. Rosheno does not request the default Phone role, call-log access, phone-identity access or audio recording. A complete app-owned ongoing/incoming call interface would require a separate default-dialer implementation with `InCallService`, not just a dial pad; see [Android default phone application requirements](https://developer.android.com/develop/connectivity/telecom/dialer-app).

Photo-contact/help flows retain their existing named confirmation and system-dialer handoff. This change applies to the Home «تماس» action and the family-number selector inside that dialer.

Tests intercept `ACTION_CALL` and `ACTION_DIAL` before any phone activity can launch. They verify keypad input, cancellation, persisted input after recreation, deletion, normalized intent data, one confirmed dispatch, unsafe input rejection and safe emergency routing. No real call or carrier charge is generated. Real SIM/network behavior remains a physical-device validation task.

[Dialer preview](../screenshots/prototype-dialer-v0.5.png).
