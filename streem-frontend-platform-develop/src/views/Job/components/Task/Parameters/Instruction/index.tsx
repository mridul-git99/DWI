import { ContentState, convertToRaw, EditorState } from 'draft-js';
import draftToHtml from 'draftjs-to-html';
import htmlToDraft from 'html-to-draftjs';
import React, { FC, useEffect, useState } from 'react';
import { Editor } from 'react-draft-wysiwyg';
import 'react-draft-wysiwyg/dist/react-draft-wysiwyg.css';
import { Wrapper } from './styles';
import { ParameterProps } from '../Parameter';

const InstructionParameter: FC<ParameterProps> = ({ parameter }) => {
  const [editorState, setEditorState] = useState(EditorState.createEmpty());
  const [contentBlock, setContentBlock] = useState(htmlToDraft(parameter.data.text));

  useEffect(() => {
    if (contentBlock) {
      const contentState = ContentState.createFromBlockArray(contentBlock.contentBlocks);

      setEditorState(EditorState.createWithContent(contentState));
    }
  }, [contentBlock]);

  useEffect(() => {
    setContentBlock(htmlToDraft(parameter.data.text));
  }, [parameter]);

  return (
    <Wrapper>
      <Editor
        editorState={editorState}
        wrapperClassName="wrapper-class"
        editorClassName="editor-class"
        toolbarClassName="toolbar-class"
        onBlur={() => {
          draftToHtml(convertToRaw(editorState.getCurrentContent()));
        }}
        onEditorStateChange={(newEditorState) => setEditorState(newEditorState)}
        data-id={parameter.id}
        data-type={parameter.type}
      />
    </Wrapper>
  );
};

export default InstructionParameter;
