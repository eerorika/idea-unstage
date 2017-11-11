/*
Copyright 2017 Eero Rikalainen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package eerorika.unstage;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.actions.GitAction;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.intellij.util.ObjectUtils.assertNotNull;
import static git4idea.GitUtil.getRepositoryManager;
import static git4idea.commands.GitHandlerUtil.doSynchronously;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.groupingBy;

/**
 * @author Eero Rikalainen
 * @version 1.0
 */
public class Unstage extends GitAction {
	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = assertNotNull(event.getProject());
		VirtualFile[] virtualFiles = assertNotNull(
			event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));

		Map<Repository, List<VirtualFile>> repositoryMap
			= stream(virtualFiles).collect(
			groupingBy(
				getRepositoryManager(project)::getRepositoryForFile
			)
		);

		repositoryMap.forEach((repository, repoFiles) -> {
			GitSimpleHandler handler = new GitSimpleHandler(
				project, repository.getRoot(), GitCommand.RESET);
			handler.endOptions();
			handler.addRelativeFiles(repoFiles);
			doSynchronously(handler, "Unstaging",
				handler.printableCommandLine());
		});

		VcsDirtyScopeManager.getInstance(project).filesDirty(
			null, Arrays.asList(virtualFiles));
	}

	private static boolean isChanged(@NotNull AnActionEvent event,
									 VirtualFile virtualFile) {
		Project project = assertNotNull(event.getProject());
		FileStatus status =
			FileStatusManager.getInstance(project).getStatus(virtualFile);
		return status != FileStatus.NOT_CHANGED;
	}

	private static <T> boolean isNotEmpty(T[] array) {
		return array != null && array.length != 0;
	}

	@Override
	protected boolean isEnabled(@NotNull AnActionEvent event) {
		VirtualFile[] virtualFiles =
			event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
		// don't bother checking multiple files; in that case assume changed
		return isNotEmpty(virtualFiles) && (
			virtualFiles.length > 1
				|| isChanged(event, virtualFiles[0])
		);
	}
}
