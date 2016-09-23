# kskandispersonal/xDrip-plus
> Added standalone mode to the Jamorham xDrip+ Wear Watchface
This enables a standalone smartwatch to connect directly to the Dexcom G5 Transmitter.  Upon reconnecting to the smartphone, all BG readings on the smartwatch are synced back up to the smartphone and the smartphone BT connection resumes.  To enable this mode, check the connectG5 preference in xDrip Prefs on the smartphone.

This feature has been tested on Sony Smartwatch 3 using the xDrip+ BigChart watchface on many long runs while not connected to my Samsung Galaxy Note 4.  Upon reestabling wear connection, my Sony Smartwatch BG data (TransmitterData.java) was sent immediately to my Note 4, and its BT connection to the Dexcom G5 resumed.  The BGs from the standalone smartwatch were reflected in the xDrip+ UI as well as my Nightscout website.  However, no testing was performed on other xDrip+ connections such as Parakeet.

TODO:
* Add xDrip+ notifications on smartwatch, and potentially predictions.
* Implement feature to clear/reset the Wear xDrip+ database - see ACTION_SYNC_DB in app/.../Home.java.
* Optimize / cleanup code:  
** Currently, App sends calibrations and CurrentSensor to Wear on each BG update.  Instead, App should send these only onPeerConnected().    
** Currently, leveraged code fro the App project is copied to the Wear project.  Instead, the leveraged code could be placed in a shared directory so that there is only a single version used by both App and Wear.

# Jamorham xDrip+
> My enhanced personal research version of xDrip

 Info page and APK download: https://jamorham.github.io/#xdrip-plus

##Features
* Natural language voice and text input of Treatments (Insulin/Carbs)
* Tasker plugin support including remote calibrations
* Instant data synchronization between phones and tablets
* Visualization of Insulin and Carb action curves.
* Forward prediction with live "Bolus Wizard"
<img align="middle" src="https://jamorham.github.io/images/jamorham-natural-language-treatments-two-web.png">

##Ethos
* Developed using Rapid Prototyping methodology
* Immediate results favoured to prove concepts
* Designed to support my personal research goals
* Community testing and collaboration appreciated!

##Roadmap
* Code clean up
* Extensible Profile configuration
* Increase automation and data backup and sync options
* More Nightscout integration

##Collaboration
I am very happy if people want to collaborate on this project. There is significant room for improvement in the code and in an ideal world changes could be cherry picked back to xDrip mainline. Please contact me if you want to get involved.

##Thanks
None of this would be possible without all the hard work of the xDrip and Nightscout communities who have developed such excellent software and allowed me to build upon it.

