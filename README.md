RoadBuddy
=========

An app made for bikers. Create paths, invite your buddies, and ride together!

Resources (in Italian, sorry):

 - [video](https://www.youtube.com/watch?v=84H-o9Ggkzw)
 - [concept](https://drive.google.com/file/d/0B19yc8dlm1XpSWlScURBUXhfSjg/view)
 - [final report](https://drive.google.com/open?id=0BwTnXnG7pvmHZUdLOVJkalFEbWc)

#### Android Dependencies
From the Android SDK manager you need to install (not sure this is the complete list):

 - Extras
   - Google Play Services
   - Android Support Repository
 - Tools
   - Android SDK Build Tools 23.0.3
 - Android 6.0 (API 23)
   - SDK Platform
   - Google APIs

#### Google APIs
The app uses some of the [Goole Maps APIs for Android](https://developers.google.com/maps/documentation/android-api/), specifically:

 - Google Maps Android API
 - Google Maps Directions API
 - Google Maps Geocoding API

You need to register there, create a new project and add these APIs, then add the API key for your project to [gradle.properties](gradle.properties)
and [strings.xml](app/src/main/res/values/strings.xml).

### Backend
Simply a postgresql database with the postgis extension. Refer to http://postgis.net/docs/postgis_installation.html if you don't want to use docker.
Not the best choice for a production app, of course, but this was just an Android programming class at University...

Build:

```
cd backend && docker build -t roadbuddy:latest .
```

Run:

```
docker run --name roadbuddy-db -d -p 54321:5432 -e POSTGRES_PASSWORD=lpsmt2016 -e POSTGRES_USER=roadbuddy roadbuddy:latest
```

Configuration of postgis (every time you create a new container, unless you persist `/var/lib/postgresql/data`):

```
PGPASSWORD=lpsmt2016 psql -h localhost -p 54321 -U roadbuddy -c "CREATE EXTENSION postgis; CREATE EXTENSION postgis_topology;"
```

# License
The source code is under the terms of the [GNU General Public License, version 3](http://www.gnu.org/licenses/gpl.html).
