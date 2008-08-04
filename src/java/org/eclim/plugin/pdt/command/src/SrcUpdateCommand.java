/**
 * Copyright (C) 2005 - 2008  Eric Van Dewoestine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclim.plugin.pdt.command.src;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.eclim.command.AbstractCommand;
import org.eclim.command.CommandLine;
import org.eclim.command.Error;
import org.eclim.command.Options;

import org.eclim.command.filter.ErrorFilter;

import org.eclim.util.IOUtils;
import org.eclim.util.ProjectUtils;

import org.eclim.util.file.FileOffsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.dltk.compiler.problem.AbstractProblemReporter;
import org.eclipse.dltk.compiler.problem.IProblem;

import org.eclipse.php.internal.core.compiler.ast.parser.PHPSourceParserFactory;

/**
 * Command to update and optionally validate a php src file.
 *
 * @author Eric Van Dewoestine (ervandew@gmail.com)
 * @version $Revision$
 */
public class SrcUpdateCommand
  extends AbstractCommand
{
  /**
   * {@inheritDoc}
   */
  public String execute (CommandLine _commandLine)
    throws Exception
  {
    String file = _commandLine.getValue(Options.FILE_OPTION);
    String projectName = _commandLine.getValue(Options.PROJECT_OPTION);

    IProject project = ProjectUtils.getProject(projectName, true);
    IFile ifile = ProjectUtils.getFile(project, file);

    // ensure model is refreshed with latest version of the file.
//    PHPWorkspaceModelManager.getInstance().addFileToModel(ifile);

    // validate the src file.
    if(_commandLine.hasOption(Options.VALIDATE_OPTION)){

      PHPSourceParserFactory parser = new PHPSourceParserFactory();
      Reporter reporter = new Reporter();
      parser.parse(
          file.toCharArray(),
          IOUtils.toString(ifile.getContents()).toCharArray(),
          reporter
      );

      String filepath = ProjectUtils.getFilePath(project, file);
      FileOffsets offsets = FileOffsets.compile(filepath);
      ArrayList<Error> errors = new ArrayList<Error>();
      for(IProblem problem : reporter.getProblems()){
        int[] lineColumn = offsets.offsetToLineColumn(problem.getSourceStart());
        errors.add(new Error(
            problem.getMessage(),
            filepath,
            lineColumn[0],
            lineColumn[1],
            problem.isWarning()
        ));
      }
      return ErrorFilter.instance.filter(_commandLine, errors);
    }
    return StringUtils.EMPTY;
  }

  private class Reporter
    extends AbstractProblemReporter
  {
    private List<IProblem> problems = new ArrayList<IProblem>();

    public void reportProblem (IProblem problem){
      problems.add(problem);
    }

    public List<IProblem> getProblems ()
    {
      return problems;
    }
  }
}
