def call(String module='.'){
	// now build, based on the configuration provided
	echo "\u2705 \u2705 \u2705 TAGGING \u2705 \u2705 \u2705"
	try { unstash "vcs.source" } catch (error_x) {}
	if (fileExists("VCS_SOURCE")) {
		def vcs = readFile "VCS_SOURCE"
		if (vcs.contains('.git')) {
			tagGIT(module)
		}
		else if (vcs.contains('SVN')) {
			tagSubversion(module)
		}
		else {
			sh 'set && ls -alrt && echo "VCS_SOURCE = $(cat VCS_SOURCE)"'
			throw new Exception("unknown vcs system, please contact your administrator.")
		}
	} else {
		sh 'set && ls -alrt '
		throw new Exception("unknown tag version control mechanism, please contact your administrator.")
	}
}
