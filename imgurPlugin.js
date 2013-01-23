function ImgurUpload(){
}

ImgurUpload.prototype.upload = function(key, fileUri, success, error){
	var params = {
		key : key,
		file: fileUri
	};
	cordova.exec(success, error, "ImgurPlugin", "upload", [params]);
}

cordova.addConstructor(function(){
	if(!window.plugins){
		window.plugins = {};
	}
	
	if (!window.plugins.imgurUpload){
		window.plugins.imgurUpload = new ImgurUpload();
	}
});
