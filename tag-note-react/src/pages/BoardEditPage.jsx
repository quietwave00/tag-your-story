import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { boardService } from '../services/boardService.js';
import { fileService } from '../services/fileService.js';
import { buildRoute } from '../utils/routes.js';
import '../styles/boardEdit.css';

const MAX_USER_TAG_COUNT = 5;
const MAX_FILE_COUNT = 3;
const ACCEPTED_IMAGE_TYPES = ['image/png', 'image/jpeg'];

function createBoardFileFormData(boardId, files) {
  const formData = new FormData();

  files.forEach((file) => {
    formData.append('fileList', file);
  });
  formData.append('boardId', boardId);

  return formData;
}

function normalizeUserTag(value) {
  return value.replace(/\s/g, '').replace(/^#/, '');
}

function validateImageFile(file) {
  return ACCEPTED_IMAGE_TYPES.includes(file.type);
}

export default function BoardEditPage() {
  const { boardId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const trackId = searchParams.get('trackId');
  const isEditMode = Boolean(boardId);
  const [content, setContent] = useState('');
  const [userTagInput, setUserTagInput] = useState('');
  const [userTags, setUserTags] = useState([]);
  const [existingFiles, setExistingFiles] = useState([]);
  const [deletedFileIds, setDeletedFileIds] = useState([]);
  const [newFiles, setNewFiles] = useState([]);
  const [isLoading, setIsLoading] = useState(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const title = isEditMode ? '게시글 수정' : '게시글 작성';
  const totalFileCount = existingFiles.length + newFiles.length;
  const canSubmit = content.trim() && userTags.length > 0 && !isSubmitting;

  const newFilePreviews = useMemo(
    () =>
      newFiles.map((file) => ({
        file,
        previewUrl: URL.createObjectURL(file),
      })),
    [newFiles],
  );

  useEffect(() => {
    return () => {
      newFilePreviews.forEach(({ previewUrl }) => URL.revokeObjectURL(previewUrl));
    };
  }, [newFilePreviews]);

  useEffect(() => {
    if (!isEditMode || !boardId) {
      return;
    }

    let ignore = false;

    async function loadBoardFormData() {
      setIsLoading(true);

      try {
        const [boardResponse, fileResponse] = await Promise.all([
          boardService.getBoardByBoardId(boardId),
          fileService.getFileList(boardId),
        ]);

        if (!ignore) {
          setContent(boardResponse?.content ?? '');
          setUserTags(boardResponse?.userTagNameList?.nameList ?? []);
          setExistingFiles(fileResponse ?? []);
          setErrorMessage('');
        }
      } catch {
        if (!ignore) {
          setErrorMessage('게시글 정보를 불러오지 못했습니다.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadBoardFormData();

    return () => {
      ignore = true;
    };
  }, [boardId, isEditMode]);

  const addUserTag = () => {
    const userTag = normalizeUserTag(userTagInput);

    if (!userTag) {
      setErrorMessage('유저 태그 내용을 입력해 주세요.');
      return;
    }

    if (userTags.length >= MAX_USER_TAG_COUNT) {
      setErrorMessage('유저 태그는 다섯 개까지 입력 가능합니다.');
      return;
    }

    if (userTags.includes(userTag)) {
      setErrorMessage('이미 입력된 유저 태그입니다.');
      setUserTagInput('');
      return;
    }

    setUserTags((currentUserTags) => [...currentUserTags, userTag]);
    setUserTagInput('');
    setErrorMessage('');
  };

  const handleUserTagKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      addUserTag();
    }
  };

  const removeUserTag = (userTag) => {
    setUserTags((currentUserTags) => currentUserTags.filter((item) => item !== userTag));
  };

  const handleFileChange = (event) => {
    const selectedFiles = Array.from(event.target.files ?? []);

    if (selectedFiles.length === 0) {
      return;
    }

    const availableCount = MAX_FILE_COUNT - totalFileCount;

    if (availableCount <= 0) {
      setErrorMessage('이미지는 3개까지 첨부 가능합니다.');
      event.target.value = '';
      return;
    }

    const validFiles = selectedFiles.filter(validateImageFile).slice(0, availableCount);

    if (validFiles.length !== selectedFiles.length) {
      setErrorMessage('png, jpg, jpeg 이미지 파일만 선택할 수 있습니다.');
    } else {
      setErrorMessage('');
    }

    setNewFiles((currentFiles) => [...currentFiles, ...validFiles]);
    event.target.value = '';
  };

  const removeExistingFile = (fileId) => {
    setExistingFiles((files) => files.filter((file) => file.fileId !== fileId));
    setDeletedFileIds((fileIds) => [...fileIds, fileId]);
  };

  const removeNewFile = (file) => {
    setNewFiles((files) => files.filter((currentFile) => currentFile !== file));
  };

  const uploadNewFiles = async (targetBoardId, requestType) => {
    if (newFiles.length === 0) {
      return;
    }

    const formData = createBoardFileFormData(targetBoardId, newFiles);

    if (requestType === 'update') {
      await fileService.update(formData);
      return;
    }

    await fileService.upload(formData);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!content.trim()) {
      setErrorMessage('이야기를 작성해 주세요.');
      return;
    }

    if (userTags.length === 0) {
      setErrorMessage('유저 태그를 하나 이상 작성해 주셔야 합니다.');
      return;
    }

    if (!isEditMode && !trackId) {
      setErrorMessage('트랙 정보가 없어 게시글을 작성할 수 없습니다.');
      return;
    }

    setIsSubmitting(true);

    try {
      if (isEditMode) {
        await boardService.updateBoardAndUserTag(boardId, content, userTags);

        if (deletedFileIds.length > 0) {
          await fileService.deleteFileList(deletedFileIds, boardId);
        }

        await uploadNewFiles(boardId, 'update');
        navigate(buildRoute.boardDetail(boardId));
        return;
      }

      const createdBoard = await boardService.writeBoard({
        content,
        userTagList: userTags,
        trackId,
      });

      await uploadNewFiles(createdBoard.boardId, 'create');
      navigate(buildRoute.boardDetail(createdBoard.boardId));
    } catch {
      setErrorMessage('게시글 저장 요청을 처리하지 못했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    if (isEditMode) {
      navigate(buildRoute.boardDetail(boardId));
      return;
    }

    navigate(-1);
  };

  if (isLoading) {
    return <p className="board-edit-message">게시글 정보를 불러오는 중입니다.</p>;
  }

  return (
    <form className="board-edit-page" onSubmit={handleSubmit}>
      <header className="board-edit-header">
        <h1>{title}</h1>
      </header>

      <section className="board-edit-section" aria-labelledby="user-tag-title">
        <h2 id="user-tag-title">유저 태그</h2>
        <div className="board-edit-user-tag-list">
          {userTags.map((userTag) => (
            <button
              className="board-edit-user-tag"
              key={userTag}
              onClick={() => removeUserTag(userTag)}
              type="button"
            >
              #{userTag}
            </button>
          ))}
        </div>
        <input
          className="board-edit-tag-input"
          onChange={(event) => setUserTagInput(event.target.value)}
          onKeyDown={handleUserTagKeyDown}
          placeholder="입력, 엔터!"
          type="text"
          value={userTagInput}
        />
      </section>

      <section className="board-edit-section" aria-labelledby="file-title">
        <h2 id="file-title">이미지</h2>
        <label className="board-file-upload">
          <input accept="image/png,image/jpeg" multiple onChange={handleFileChange} type="file" />
          <span>Upload Image</span>
        </label>
        <div className="board-file-preview-list">
          {existingFiles.map((file) => (
            <button
              className="board-file-preview"
              key={file.fileId}
              onClick={() => removeExistingFile(file.fileId)}
              type="button"
            >
              <img src={file.filePath} alt="uploaded" />
            </button>
          ))}
          {newFilePreviews.map(({ file, previewUrl }) => (
            <button
              className="board-file-preview"
              key={`${file.name}-${file.lastModified}`}
              onClick={() => removeNewFile(file)}
              type="button"
            >
              <img src={previewUrl} alt={file.name} />
            </button>
          ))}
        </div>
      </section>

      <section className="board-edit-section" aria-labelledby="content-title">
        <h2 id="content-title">이야기</h2>
        <textarea
          className="board-edit-content"
          onChange={(event) => setContent(event.target.value)}
          placeholder="이야기를 작성해 주세요."
          value={content}
        />
      </section>

      {errorMessage ? <p className="board-edit-message error">{errorMessage}</p> : null}

      <footer className="board-edit-footer">
        <button className="secondary-button" onClick={handleCancel} type="button">
          Cancel
        </button>
        <button className="primary-button" disabled={!canSubmit} type="submit">
          {isEditMode ? 'Edit' : 'Write'}
        </button>
      </footer>
    </form>
  );
}
