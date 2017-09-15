# Copyright (c) 2015, CodiLime Inc.

import base64
from datetime import datetime

from nbformat import reads, writes, from_dict
from notebook.services.contents.manager import ContentsManager
from tornado import web
from traitlets import Unicode

from seahorse_notebook_path import SeahorseNotebookPath
from .wmcheckpoints import WMCheckpoints

try:
    from urllib.request import urlopen, Request
    from urllib.error import HTTPError
except ImportError:
    from urllib2 import urlopen, Request
    from urllib2 import HTTPError

NBFORMAT_VERSION = 4
DUMMY_CREATED_DATE = datetime.fromtimestamp(0)


class WMContentsManager(ContentsManager):

    KERNEL_TYPES = {
        'r': {
            'display_name': 'SparkR',
            'name': 'forwarding_kernel_r',
            'version': '3.2.3'
        },
        'python': {
            'display_name': 'PySpark',
            'name': 'forwarding_kernel_py',
            'version': '2.7.10'
        }
    }

    workflow_manager_url = Unicode(
        default_value="http://localhost:9080",
        allow_none=False,
        config=True,
        help="Workflow Manager URL",
    )

    workflow_manager_user = Unicode(
        default_value="",
        allow_none=False,
        config=True,
        help="Workflow Manager auth user",
    )

    workflow_manager_pass = Unicode(
        default_value="",
        allow_none=False,
        config=True,
        help="Workflow Manager auth pass",
    )

    def _checkpoints_class_default(self):
        return WMCheckpoints

    def _get_wm_notebook_url(self, path):
        return "{}/v1/workflows/{}/notebook/{}".format(
                self.workflow_manager_url, path.workflow_id, path.node_id)

    def _create_request(self, url):
        req = Request(url)
        username = self.workflow_manager_user
        password = self.workflow_manager_pass
        credentials = '%s:%s' % (username, password)
        base64string = base64.encodestring(credentials.encode()).decode('utf-8').replace('\n', '')
        req.add_header("Authorization", "Basic %s" % base64string)
        req.add_header("X-Seahorse-UserId", "notebook")
        req.add_header("X-Seahorse-UserName", "notebook")
        return req

    def create_model(self, content_json, path):
        return {
            "name": "Seahorse Editor Notebook",
            "path": path.serialize(),
            "type": "notebook",
            "writable": True,
            "last_modified": DUMMY_CREATED_DATE,
            "created": DUMMY_CREATED_DATE,
            "content": reads(content_json, NBFORMAT_VERSION) if content_json is not None else None,
            "format": "json" if content_json is not None else None,
            "mimetype": None,
        }

    def _create_notebook(self, notebook_info):

        return {
            "cells": [],
            "metadata": {
                "kernelspec": {
                    "display_name": self.KERNEL_TYPES[notebook_info.language]['display_name'],
                    "name": self.KERNEL_TYPES[notebook_info.language]['name'],
                    "language": notebook_info.language
                },
                "language_info": {
                    "name": notebook_info.language,
                    "version": self.KERNEL_TYPES[notebook_info.language]['version']
                }
            },
            "nbformat": NBFORMAT_VERSION,
            "nbformat_minor": 0
        }

    def _save_notebook(self, path, content_json, return_content=False):
        try:
            response = urlopen(self._create_request(self._get_wm_notebook_url(path)), content_json.encode("utf-8"))
            if response.getcode() == 201:
                return self.create_model(content_json if return_content else None, path)
            else:
                raise web.HTTPError(response.status, response.msg)
        except web.HTTPError:
            raise
        except HTTPError as e:
            raise web.HTTPError(e.code, e.msg)
        except Exception as e:
            raise web.HTTPError(500, str(e))

    def get(self, path, content=True, type=None, format=None):
        assert isinstance(path, str)
        try:
            notebook_info = SeahorseNotebookPath.deserialize(path)
        except SeahorseNotebookPath.DeserializationFailed as e:
            raise web.HTTPError(400, e.message)

        try:
            response = urlopen(self._create_request(self._get_wm_notebook_url(notebook_info)))
            if response.getcode() == 200:
                content_json = response.read().decode("utf-8")
                return self.create_model(content_json if content else None, notebook_info)
            else:
                raise web.HTTPError(response.status, response.msg)
        except web.HTTPError:
            raise
        except HTTPError as e:
            if e.code == 404:
                content_json = writes(from_dict(self._create_notebook(notebook_info)), NBFORMAT_VERSION)
                return self._save_notebook(notebook_info, content_json, content)
            else:
                raise web.HTTPError(e.code, e.msg)
        except Exception as e:
            raise web.HTTPError(500, str(e))

    def save(self, model, path):
        assert isinstance(path, str)
        try:
            notebook_info = SeahorseNotebookPath.deserialize(path)
        except SeahorseNotebookPath.DeserializationFailed as e:
            raise web.HTTPError(400, e.message)

        if model['type'] != "notebook":
            model['message'] = "Cannot save object of type: {}".format(model['type'])
            return model

        content_json = writes(from_dict(model['content']), NBFORMAT_VERSION)
        return self._save_notebook(notebook_info, content_json, False)

    def delete_file(self, path):
        raise web.HTTPError(400, "Unsupported: delete_file {}".format(path))

    def rename_file(self, old_path, path):
        raise web.HTTPError(400, "Unsupported: rename_file {} {}".format(old_path, path))

    def file_exists(self, path):
        try:
            self.get(path)
            return True
        except:
            return False

    def dir_exists(self, path):
        return False

    def is_hidden(self, path):
        return False
