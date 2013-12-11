var deployJava = function() {
   var hattrs = {core : ['id', 'class', 'title', 'style'], i18n : ['lang', 'dir'], events : ['onclick', 'ondblclick', 'onmousedown', 'onmouseup', 'onmouseover', 'onmousemove', 'onmouseout', 'onkeypress', 'onkeydown', 'onkeyup'], applet : ['codebase', 'code', 'name', 'archive', 'object', 'width', 'height', 'alt', 'align', 'hspace', 'vspace'], object : ['classid', 'codebase', 'codetype', 'data', 'type', 'archive', 'declare', 'standby', 'height', 'width', 'usemap', 'name', 'tabindex', 'align', 'border', 'hspace', 'vspace']};
   var object_valid_attrs = hattrs.object.concat(hattrs.core, hattrs.i18n, hattrs.events);
   var applet_valid_attrs = hattrs.applet.concat(hattrs.core);

   function arHas(ar, attr) {
      var len = ar.length;
      for (var i = 0; i < len; i++) {
         if (ar[i] === attr) {
            return true;
         }
      }
      return false;
   }

   function isValidAppletAttr(attr) {
      return arHas(applet_valid_attrs, attr.toLowerCase());
   }

   function isValidObjectAttr(attr) {
      return arHas(object_valid_attrs, attr.toLowerCase());
   }

   var rv = {debug : null, firefoxJavaVersion : null, myInterval : null, preInstallJREList : null, returnPage : null, brand : null, locale : null, installType : null, EAInstallEnabled : false, EarlyAccessURL : null, getJavaURL : 'http://java.sun.com/webapps/getjava/BrowserRedirect?host=java.com', appleRedirectPage : 'http://www.apple.com/support/downloads/', oldMimeType : 'application/npruntime-scriptable-plugin;DeploymentToolkit', mimeType : 'application/java-deployment-toolkit', launchButtonPNG : 'images/webstart.png', browserName : null, browserName2 : null, getJREs : function() {
      var list = new Array();
      if (this.isPluginInstalled()) {
         var plugin = this.getPlugin();
         var VMs = plugin.jvms;
         for (var i = 0; i < VMs.getLength(); i++) {
            list[i] = VMs.get(i).version;
         }
      }
      else {
         var browser = this.getBrowser();
         if (browser == 'MSIE') {
            if (this.testUsingActiveX('1.7.0')) {
               list[0] = '1.7.0';
            }
            else if (this.testUsingActiveX('1.6.0')) {
               list[0] = '1.6.0';
            }
            else if (this.testUsingActiveX('1.5.0')) {
               list[0] = '1.5.0';
            }
            else if (this.testUsingActiveX('1.4.2')) {
               list[0] = '1.4.2';
            }
            else if (this.testForMSVM()) {
               list[0] = '1.1';
            }
         }
         else if (browser == 'Netscape Family') {
            this.getJPIVersionUsingMimeType();
            if (this.firefoxJavaVersion != null) {
               list[0] = this.firefoxJavaVersion;
            }
            else if (this.testUsingMimeTypes('1.7')) {
               list[0] = '1.7.0';
            }
            else if (this.testUsingMimeTypes('1.6')) {
               list[0] = '1.6.0';
            }
            else if (this.testUsingMimeTypes('1.5')) {
               list[0] = '1.5.0';
            }
            else if (this.testUsingMimeTypes('1.4.2')) {
               list[0] = '1.4.2';
            }
            else if (this.browserName2 == 'Safari') {
               if (this.testUsingPluginsArray('1.7.0')) {
                  list[0] = '1.7.0';
               }
               else if (this.testUsingPluginsArray('1.6')) {
                  list[0] = '1.6.0';
               }
               else if (this.testUsingPluginsArray('1.5')) {
                  list[0] = '1.5.0';
               }
               else if (this.testUsingPluginsArray('1.4.2')) {
                  list[0] = '1.4.2';
               }
            }
         }
      }
      if (this.debug) {
         for (var i = 0; i < list.length; ++i) {
            alert('We claim to have detected Java SE ' + list[i]);
         }
      }
      return list;
   }, installJRE : function(requestVersion, installCallback) {
      var ret = false;
      if (this.isPluginInstalled()) {
         if (this.getPlugin().installJRE(requestVersion, installCallback)) {
            this.refresh();
            if (this.returnPage != null) {
               document.location = this.returnPage;
            }
            return true;
         }
         else {
            return false;
         }
      }
      else {
         return this.installLatestJRE();
      }
   }, installLatestJRE : function(installCallback) {
      if (this.isPluginInstalled()) {
         if (this.getPlugin().installLatestJRE(installCallback)) {
            this.refresh();
            if (this.returnPage != null) {
               document.location = this.returnPage;
            }
            return true;
         }
         else {
            return false;
         }
      }
      else {
         var browser = this.getBrowser();
         var platform = navigator.platform.toLowerCase();
         if ((this.EAInstallEnabled == 'true') && (platform.indexOf('win') != -1) && (this.EarlyAccessURL != null)) {
            this.preInstallJREList = this.getJREs();
            if (this.returnPage != null) {
               this.myInterval = setInterval("deployJava.poll()", 3000);
            }
            location.href = this.EarlyAccessURL;
            return false;
         }
         else {
            if (browser == 'MSIE') {
               return this.IEInstall();
            }
            else if ((browser == 'Netscape Family') && (platform.indexOf('win32') != -1)) {
               return this.FFInstall();
            }
            else {
               location.href = this.getJavaURL +
                               ((this.returnPage != null) ? ('&returnPage=' + this.returnPage) : '') +
                               ((this.locale != null) ? ('&locale=' + this.locale) : '') +
                               ((this.brand != null) ? ('&brand=' + this.brand) : '');
            }
            return false;
         }
      }
   }, runApplet : function(attributes, parameters, minimumVersion) {
      if (minimumVersion == 'undefined' || minimumVersion == null) {
         minimumVersion = '1.1';
      }
      var regex = "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";
      var matchData = minimumVersion.match(regex);
      if (this.returnPage == null) {
         this.returnPage = document.location;
      }
      if (matchData != null) {
         var browser = this.getBrowser();
         if ((browser != '?') && ('Safari' != this.browserName2)) {
            if (this.versionCheck(minimumVersion + '+')) {
               this.writeAppletTag(attributes, parameters);
            }
            else if (this.installJRE(minimumVersion + '+')) {
               this.refresh();
               location.href = document.location;
               this.writeAppletTag(attributes, parameters);
            }
         }
         else {
            this.writeAppletTag(attributes, parameters);
         }
      }
      else {
         if (this.debug) {
            alert('Invalid minimumVersion argument to runApplet():' +
                  minimumVersion);
         }
      }
   }, writeAppletTag : function(attributes, parameters) {
      var startApplet = '<' + 'applet ';
      var params = '';
      var endApplet = '<' + '/' + 'applet' + '>';
      var addCodeAttribute = true;
      if (null == parameters || typeof parameters != 'object') {
         parameters = new Object();
      }
      for (var attribute in attributes) {
         if (!isValidAppletAttr(attribute)) {
            parameters[attribute] = attributes[attribute];
         }
         else {
            startApplet += (' ' + attribute + '="' + attributes[attribute] + '"');
            if (attribute == 'code') {
               addCodeAttribute = false;
            }
         }
      }
      var codebaseParam = false;
      for (var parameter in parameters) {
         if (parameter == 'codebase_lookup') {
            codebaseParam = true;
         }
         if (parameter == 'object' || parameter == 'java_object' || parameter == 'java_code') {
            addCodeAttribute = false;
         }
         params += '<param name="' + parameter + '" value="' +
                   parameters[parameter] + '"/>';
      }
      if (!codebaseParam) {
         params += '<param name="codebase_lookup" value="false"/>';
      }
      if (addCodeAttribute) {
         startApplet += (' code="dummy"');
      }
      startApplet += '>';
      document.write(startApplet + '\n' + params + '\n' + endApplet);
   }, versionCheck : function(versionPattern) {
      var index = 0;
      var regex = "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?(\\*|\\+)?$";
      var matchData = versionPattern.match(regex);
      if (matchData != null) {
         var familyMatch = true;
         var patternArray = new Array();
         for (var i = 1; i < matchData.length; ++i) {
            if ((typeof matchData[i] == 'string') && (matchData[i] != '')) {
               patternArray[index] = matchData[i];
               index++;
            }
         }
         if (patternArray[patternArray.length - 1] == '+') {
            familyMatch = false;
            patternArray.length--;
         }
         else {
            if (patternArray[patternArray.length - 1] == '*') {
               patternArray.length--;
            }
         }
         var list = this.getJREs();
         for (var i = 0; i < list.length; ++i) {
            if (this.compareVersionToPattern(list[i], patternArray, familyMatch)) {
               return true;
            }
         }
         return false;
      }
      else {
         alert('Invalid versionPattern passed to versionCheck: ' +
               versionPattern);
         return false;
      }
   }, isWebStartInstalled : function(minimumVersion) {
      var browser = this.getBrowser();
      if ((browser == '?') || ('Safari' == this.browserName2)) {
         return true;
      }
      if (minimumVersion == 'undefined' || minimumVersion == null) {
         minimumVersion = '1.4.2';
      }
      var retval = false;
      var regex = "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";
      var matchData = minimumVersion.match(regex);
      if (matchData != null) {
         retval = this.versionCheck(minimumVersion + '+');
      }
      else {
         if (this.debug) {
            alert('Invalid minimumVersion argument to isWebStartInstalled(): ' + minimumVersion);
         }
         retval = this.versionCheck('1.4.2+');
      }
      return retval;
   }, getJPIVersionUsingMimeType : function() {
      for (var i = 0; i < navigator.mimeTypes.length; ++i) {
         var s = navigator.mimeTypes[i].type;
         var m = s.match(/^application\/x-java-applet;jpi-version=(.*)$/);
         if (m != null) {
            this.firefoxJavaVersion = m[1];
            if ('Opera' != this.browserName2) {
               break;
            }
         }
      }
   }, launchWebStartApplication : function(jnlp) {
      var uaString = navigator.userAgent.toLowerCase();
      this.getJPIVersionUsingMimeType();
      if (this.isWebStartInstalled('1.7.0') == false) {
         if ((this.installJRE('1.7.0+') == false) || ((this.isWebStartInstalled('1.7.0') == false))) {
            return false;
         }
      }
      var jnlpDocbase = null;
      if (document.documentURI) {
         jnlpDocbase = document.documentURI;
      }
      if (jnlpDocbase == null) {
         jnlpDocbase = document.URL;
      }
      var browser = this.getBrowser();
      var launchTag;
      if (browser == 'MSIE') {
         launchTag = '<' + 'object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" ' + 'width="0" height="0">' + '<' + 'PARAM name="launchjnlp" value="' + jnlp + '"' + '>' + '<' + 'PARAM name="docbase" value="' + jnlpDocbase + '"' + '>' + '<' + '/' + 'object' + '>';
      }
      else if (browser == 'Netscape Family') {
         launchTag = '<' + 'embed type="application/x-java-applet;jpi-version=' +
                     this.firefoxJavaVersion + '" ' + 'width="0" height="0" ' + 'launchjnlp="' + jnlp + '"' + 'docbase="' + jnlpDocbase + '"' + ' />';
      }
      if (document.body == 'undefined' || document.body == null) {
         document.write(launchTag);
         document.location = jnlpDocbase;
      }
      else {
         var divTag = document.createElement("div");
         divTag.id = "div1";
         divTag.style.position = "relative";
         divTag.style.left = "-10000px";
         divTag.style.margin = "0px auto";
         divTag.className = "dynamicDiv";
         divTag.innerHTML = launchTag;
         document.body.appendChild(divTag);
      }
   }, createWebStartLaunchButtonEx : function(jnlp, minimumVersion) {
      if (this.returnPage == null) {
         this.returnPage = jnlp;
      }
      var url = 'javascript:deployJava.launchWebStartApplication(\'' + jnlp + '\');';
      document.write('<' + 'a href="' + url + '" onMouseOver="window.status=\'\'; ' + 'return true;"><' + 'img ' + 'src="' + this.launchButtonPNG + '" ' + 'border="0" /><' + '/' + 'a' + '>');
   }, createWebStartLaunchButton : function(jnlp, minimumVersion) {
      if (this.returnPage == null) {
         this.returnPage = jnlp;
      }
      var url = 'javascript:' + 'if (!deployJava.isWebStartInstalled(&quot;' +
                minimumVersion + '&quot;)) {' + 'if (deployJava.installLatestJRE()) {' + 'if (deployJava.launch(&quot;' + jnlp + '&quot;)) {}' + '}' + '} else {' + 'if (deployJava.launch(&quot;' + jnlp + '&quot;)) {}' + '}';
      document.write('<' + 'a href="' + url + '" onMouseOver="window.status=\'\'; ' + 'return true;"><' + 'img ' + 'src="' + this.launchButtonPNG + '" ' + 'border="0" /><' + '/' + 'a' + '>');
   }, launch : function(jnlp) {
      document.location = jnlp;
      return true;
   }, isPluginInstalled : function() {
      var plugin = this.getPlugin();
      if (plugin && plugin.jvms) {
         return true;
      }
      else {
         return false;
      }
   }, isAutoUpdateEnabled : function() {
      if (this.isPluginInstalled()) {
         return this.getPlugin().isAutoUpdateEnabled();
      }
      return false;
   }, setAutoUpdateEnabled : function() {
      if (this.isPluginInstalled()) {
         return this.getPlugin().setAutoUpdateEnabled();
      }
      return false;
   }, setInstallerType : function(type) {
      this.installType = type;
      if (this.isPluginInstalled()) {
         return this.getPlugin().setInstallerType(type);
      }
      return false;
   }, setAdditionalPackages : function(packageList) {
      if (this.isPluginInstalled()) {
         return this.getPlugin().setAdditionalPackages(packageList);
      }
      return false;
   }, setEarlyAccess : function(enabled) {
      this.EAInstallEnabled = enabled;
   }, isPlugin2 : function() {
      if (this.isPluginInstalled()) {
         if (this.versionCheck('1.6.0_10+')) {
            try {
               return this.getPlugin().isPlugin2();
            }
            catch (err) {
            }
         }
      }
      return false;
   }, allowPlugin : function() {
      this.getBrowser();
      var ret = ('Safari' != this.browserName2 && 'Opera' != this.browserName2);
      return ret;
   }, getPlugin : function() {
      this.refresh();
      var ret = null;
      if (this.allowPlugin()) {
         ret = document.getElementById('deployJavaPlugin');
      }
      return ret;
   }, compareVersionToPattern : function(version, patternArray, familyMatch) {
      var regex = "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";
      var matchData = version.match(regex);
      if (matchData != null) {
         var index = 0;
         var result = new Array();
         for (var i = 1; i < matchData.length; ++i) {
            if ((typeof matchData[i] == 'string') && (matchData[i] != '')) {
               result[index] = matchData[i];
               index++;
            }
         }
         var l = Math.min(result.length, patternArray.length);
         if (familyMatch) {
            for (var i = 0; i < l; ++i) {
               if (result[i] != patternArray[i]) {
                  return false;
               }
            }
            return true;
         }
         else {
            for (var i = 0; i < l; ++i) {
               if (result[i] < patternArray[i]) {
                  return false;
               }
               else if (result[i] > patternArray[i]) {
                  return true;
               }
            }
            return true;
         }
      }
      else {
         return false;
      }
   }, getBrowser : function() {
      if (this.browserName == null) {
         var browser = navigator.userAgent.toLowerCase();
         if (this.debug) {
            alert('userAgent -> ' + browser);
         }
         if (browser.indexOf('msie') != -1) {
            this.browserName = 'MSIE';
            this.browserName2 = 'MSIE';
         }
         else if (browser.indexOf('iphone') != -1) {
            this.browserName = 'Netscape Family';
            this.browserName2 = 'iPhone';
         }
         else if (browser.indexOf('firefox') != -1) {
            this.browserName = 'Netscape Family';
            this.browserName2 = 'Firefox';
         }
         else if (browser.indexOf('chrome') != -1) {
            this.browserName = 'Netscape Family';
            this.browserName2 = 'Chrome';
         }
         else if (browser.indexOf('safari') != -1) {
            this.browserName = 'Netscape Family';
            this.browserName2 = 'Safari';
         }
         else if (browser.indexOf('mozilla') != -1) {
            this.browserName = 'Netscape Family';
            this.browserName2 = 'Other';
         }
         else if (browser.indexOf('opera') != -1) {
            this.browserName = 'Netscape Family';
            this.browserName2 = 'Opera';
         }
         else {
            this.browserName = '?';
            this.browserName2 = 'unknown';
         }
         if (this.debug) {
            alert('Detected browser name:' + this.browserName + ', ' + this.browserName2);
         }
      }
      return this.browserName;
   }, testUsingActiveX : function(version) {
      var objectName = 'JavaWebStart.isInstalled.' + version + '.0';
      if (!ActiveXObject) {
         if (this.debug) {
            alert('Browser claims to be IE, but no ActiveXObject object?');
         }
         return false;
      }
      try {
         return(new ActiveXObject(objectName) != null);
      }
      catch (exception) {
         return false;
      }
   }, testForMSVM : function() {
      var clsid = '{08B0E5C0-4FCB-11CF-AAA5-00401C608500}';
      if (typeof oClientCaps != 'undefined') {
         var v = oClientCaps.getComponentVersion(clsid, "ComponentID");
         if ((v == '') || (v == '5,0,5000,0')) {
            return false;
         }
         else {
            return true;
         }
      }
      else {
         return false;
      }
   }, testUsingMimeTypes : function(version) {
      if (!navigator.mimeTypes) {
         if (this.debug) {
            alert('Browser claims to be Netscape family, but no mimeTypes[] array?');
         }
         return false;
      }
      for (var i = 0; i < navigator.mimeTypes.length; ++i) {
         s = navigator.mimeTypes[i].type;
         var m = s.match(/^application\/x-java-applet\x3Bversion=(1\.8|1\.7|1\.6|1\.5|1\.4\.2)$/);
         if (m != null) {
            if (this.compareVersions(m[1], version)) {
               return true;
            }
         }
      }
      return false;
   }, testUsingPluginsArray : function(version) {
      if ((!navigator.plugins) || (!navigator.plugins.length)) {
         return false;
      }
      var platform = navigator.platform.toLowerCase();
      for (var i = 0; i < navigator.plugins.length; ++i) {
         s = navigator.plugins[i].description;
         if (s.search(/^Java Switchable Plug-in (Cocoa)/) != -1) {
            if (this.compareVersions("1.5.0", version)) {
               return true;
            }
         }
         else if (s.search(/^Java/) != -1) {
            if (platform.indexOf('win') != -1) {
               if (this.compareVersions("1.5.0", version) || this.compareVersions("1.6.0", version)) {
                  return true;
               }
            }
         }
      }
      if (this.compareVersions("1.5.0", version)) {
         return true;
      }
      return false;
   }, IEInstall : function() {
      location.href = this.getJavaURL +
                      ((this.returnPage != null) ? ('&returnPage=' + this.returnPage) : '') +
                      ((this.locale != null) ? ('&locale=' + this.locale) : '') +
                      ((this.brand != null) ? ('&brand=' + this.brand) : '');
      return false;
   }, done : function(name, result) {
   }, FFInstall : function() {
      location.href = this.getJavaURL +
                      ((this.returnPage != null) ? ('&returnPage=' + this.returnPage) : '') +
                      ((this.locale != null) ? ('&locale=' + this.locale) : '') +
                      ((this.brand != null) ? ('&brand=' + this.brand) : '') +
                      ((this.installType != null) ? ('&type=' + this.installType) : '');
      return false;
   }, compareVersions : function(installed, required) {
      var a = installed.split('.');
      var b = required.split('.');
      for (var i = 0; i < a.length; ++i) {
         a[i] = Number(a[i]);
      }
      for (var i = 0; i < b.length; ++i) {
         b[i] = Number(b[i]);
      }
      if (a.length == 2) {
         a[2] = 0;
      }
      if (a[0] > b[0]) {
         return true;
      }
      if (a[0] < b[0]) {
         return false;
      }
      if (a[1] > b[1]) {
         return true;
      }
      if (a[1] < b[1]) {
         return false;
      }
      if (a[2] > b[2]) {
         return true;
      }
      if (a[2] < b[2]) {
         return false;
      }
      return true;
   }, enableAlerts : function() {
      this.browserName = null;
      this.debug = true;
   }, poll : function() {
      this.refresh();
      var postInstallJREList = this.getJREs();
      if ((this.preInstallJREList.length == 0) && (postInstallJREList.length != 0)) {
         clearInterval(this.myInterval);
         if (this.returnPage != null) {
            location.href = this.returnPage;
         }
         ;
      }
      if ((this.preInstallJREList.length != 0) && (postInstallJREList.length != 0) && (this.preInstallJREList[0] != postInstallJREList[0])) {
         clearInterval(this.myInterval);
         if (this.returnPage != null) {
            location.href = this.returnPage;
         }
      }
   }, writePluginTag : function() {
      var browser = this.getBrowser();
      if (browser == 'MSIE') {
         document.write('<' + 'object classid="clsid:CAFEEFAC-DEC7-0000-0000-ABCDEFFEDCBA" ' + 'id="deployJavaPlugin" width="0" height="0">' + '<' + '/' + 'object' + '>');
      }
      else if (browser == 'Netscape Family' && this.allowPlugin()) {
         this.writeEmbedTag();
      }
   }, refresh : function() {
      navigator.plugins.refresh(false);
      var browser = this.getBrowser();
      if (browser == 'Netscape Family' && this.allowPlugin()) {
         var plugin = document.getElementById('deployJavaPlugin');
         if (plugin == null) {
            this.writeEmbedTag();
         }
      }
   }, writeEmbedTag : function() {
      var written = false;
      if (navigator.mimeTypes != null) {
         for (var i = 0; i < navigator.mimeTypes.length; i++) {
            if (navigator.mimeTypes[i].type == this.mimeType) {
               if (navigator.mimeTypes[i].enabledPlugin) {
                  document.write('<' + 'embed id="deployJavaPlugin" type="' +
                                 this.mimeType + '" hidden="true" />');
                  written = true;
               }
            }
         }
         if (!written) {
            for (var i = 0; i < navigator.mimeTypes.length; i++) {
               if (navigator.mimeTypes[i].type == this.oldMimeType) {
                  if (navigator.mimeTypes[i].enabledPlugin) {
                     document.write('<' + 'embed id="deployJavaPlugin" type="' +
                                    this.oldMimeType + '" hidden="true" />');
                  }
               }
            }
         }
      }
   }};
   rv.writePluginTag();
   if (rv.locale == null) {
      var loc = null;
      if (loc == null) {
         try {
            loc = navigator.userLanguage;
         }
         catch (err) {
         }
      }
      if (loc == null) {
         try {
            loc = navigator.systemLanguage;
         }
         catch (err) {
         }
      }
      if (loc == null) {
         try {
            loc = navigator.language;
         }
         catch (err) {
         }
      }
      if (loc != null) {
         loc.replace("-", "_")
         rv.locale = loc;
      }
   }
   return rv;
}();